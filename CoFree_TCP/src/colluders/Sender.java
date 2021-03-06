package colluders;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

import util.Log;
import util.Message;
import util.MessageType;
import util.NetUtil;
import util.Node;

// Sender shares outMessages with main program
// Take data and make a message from it
public class Sender extends Thread implements Runnable {

    private int selfId;
    public LinkedBlockingQueue<Message> outMessages;
    private ArrayList<Node> nodesList;
    private int updatesLen;
    public boolean mustStop;
    private HashMap<Integer, SocketChannel> peers_sockets;

    private Key publicKey;
    public Peer peer;

    private static final boolean OUTPUT = true;

    // Constructor
    public Sender(int selfId, LinkedBlockingQueue<Message> outMessages,
            int updatesLen, ArrayList<Node> nodesList) {
        this.selfId = selfId;
        this.mustStop = false;
        this.outMessages = outMessages;
        this.nodesList = nodesList;
        this.updatesLen = updatesLen;

        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.genKeyPair();
            publicKey = kp.getPublic();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        this.peers_sockets = new HashMap<Integer, SocketChannel>();
    }

    public Sender(int selfId, LinkedBlockingQueue<Message> outMessages,
            int updatesLen, ArrayList<Node> nodesList, Peer peer) {
        this(selfId, outMessages, updatesLen, nodesList);
        this.peer = peer;
    }

    public void computeOneAuthenticator() {
        try {
            byte[] data = new byte[Log.HASH_SIZE + Log.LOG_ENTRY_ID_SIZE];
            new Random().nextBytes(data);
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey); // initialize object's
            // mode and key
            cipher.doFinal(data); // use object for encryption
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Take one out message if it exists and send it
    public void run() {
        int interval = 10;
        long uploadSize = 0;
        long uploadUpdateSize = 0;
        long uploadLogSize = 0;

        long initialTime = System.currentTimeMillis() / 1000;
        long lastMeasure = initialTime;

        try {
            BufferedWriter outBandwidth = null;
            if (OUTPUT && selfId != 0) {
                outBandwidth = new BufferedWriter(new FileWriter(selfId
                        + "_uploadBandwidth.txt"));
                outBandwidth.write(selfId + "\n");
            }

            while (!mustStop) {
                if (OUTPUT && selfId != 0) {
                    int nodeState = peer.getNodeStatus();
                    // Time downloadSize downloadUpdateSize downloadLogSize
                    if (System.currentTimeMillis() / 1000 - lastMeasure >= interval) {
                        lastMeasure = System.currentTimeMillis() / 1000;
                        outBandwidth.write((lastMeasure - initialTime) + " "
                                + nodeState + " "
                                + ((8 * uploadSize) / (1000 * interval)) + " "
                                + ((8 * uploadUpdateSize) / (1000 * interval))
                                + " "
                                + ((8 * uploadLogSize) / (1000 * interval))
                                + "\n");
                        uploadSize = 0;
                        uploadUpdateSize = 0;
                        uploadLogSize = 0;
                    }
                }

                // Limit bandwidth at 1000 kbps
                if (8 * uploadSize
                        / (1000 * (System.currentTimeMillis() - lastMeasure)) < 1000) {
                    Message sendMessage = outMessages.poll(10,
                            TimeUnit.MILLISECONDS);
                    if (sendMessage != null) {
                        Integer key = new Integer(sendMessage.receiverId);
                        if (!peers_sockets.containsKey(key)) {
                            connectToNode(sendMessage.receiverId);
                        }
                        SocketChannel sc = peers_sockets.get(key);

                        // 1 - create and send message
                        if (sendMessage.messageType == MessageType.SERVE
                                || sendMessage.messageType == MessageType.PROPOSE
                                || sendMessage.messageType == MessageType.REQUEST)
                            computeOneAuthenticator();

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(sendMessage);
                        oos.flush();
                        byte[] Buf = baos.toByteArray();

                        int s = NetUtil.sendMessage(Buf, sc);
                        if (sendMessage.messageType != MessageType.NODE_ABSENT
                                && sendMessage.messageType != MessageType.EVICTION_NOTIFICATION)
                            uploadSize += s;

                        // 2 - Create fake packets to consume bandwidth
                        // AdditionalSize is originally in bits, we change it to
                        // bytes
                        int sizeToSend = (int) Math
                                .ceil((double) (sendMessage.additionalSize)
                                        / (double) 8);

                        if (sendMessage.messageType == MessageType.PROPOSE
                                || sendMessage.messageType == MessageType.REQUEST
                                || sendMessage.messageType == MessageType.SERVE
                                || sendMessage.messageType == MessageType.AUDIT
                                || sendMessage.messageType == MessageType.AUDIT_RESP)
                            sizeToSend += (int) Math
                            .ceil((double) Log.AUTHENTICATOR_SIZE
                                    / (double) 8);

                        // TODO: is it necessary to send more authenticators?

                        Message emptyMsg = new Message(sendMessage.roundId,
                                MessageType.EMPTY_LOG, (short) 0, (short) 0);
                        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                        ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
                        oos2.writeObject(emptyMsg);
                        oos2.flush();

                        byte[] tmp = baos2.toByteArray();
                        byte[] data = new byte[(tmp.length > sizeToSend ? tmp.length
                                : sizeToSend)];
                        System.arraycopy(tmp, 0, data, 0, tmp.length);

                        s = NetUtil.sendMessage(data, sc);
                        uploadSize += s;
                        uploadLogSize += s;

                        // 3. create and send updates
                        sizeToSend = 0;
                        if (sendMessage.messageType == MessageType.SERVE) {
                            sizeToSend += sendMessage.getUpdatesIdSize()
                                    * updatesLen;
                        }

                        emptyMsg = new Message(sendMessage.roundId,
                                MessageType.EMPTY_UPDATES, (short) 0, (short) 0);
                        baos2 = new ByteArrayOutputStream();
                        oos2 = new ObjectOutputStream(baos2);
                        oos2.writeObject(emptyMsg);
                        oos2.flush();

                        tmp = baos2.toByteArray();
                        data = new byte[(tmp.length > sizeToSend ? tmp.length
                                : sizeToSend)];
                        System.arraycopy(tmp, 0, data, 0, tmp.length);

                        s = NetUtil.sendMessage(data, sc);
                        uploadSize += s;
                        uploadLogSize += s;
                    }
                }
            }

            if (OUTPUT && selfId != 0)
                outBandwidth.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void connectToNode(int receiverId) {
        Node nodeDest = nodesList.get(receiverId);
        InetAddress dest = nodeDest.address;
        int destPort = nodeDest.port;

        System.out.println("Node " + selfId + " connects to node " + receiverId
                + " at " + dest + ":" + destPort);

        SocketAddress address = new InetSocketAddress(dest, destPort);
        SocketChannel s = null;
        while (s == null) {
            try {
                s = SocketChannel.open(address);
                s.socket().setTcpNoDelay(true);
            } catch (IOException e) {
                s = null;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                }
            }
        }
        peers_sockets.put(new Integer(receiverId), s);
    }
}
