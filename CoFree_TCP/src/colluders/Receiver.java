package colluders;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import util.Message;
import util.MessageType;
import util.NetUtil;

class Receiver extends Thread implements Runnable {

    // Attributes
    private Peer peer;
    private int selfId;
    public LinkedBlockingQueue<Message> inMessages;
    public boolean mustStop;
    private int listeningPort;

    private static final boolean OUTPUT = true;

    // Constructor
    public Receiver(int listeningPort, LinkedBlockingQueue<Message> inMessages,
            int selfId, Peer peer) {
        this.mustStop = false;
        this.peer = peer;
        this.selfId = selfId;
        this.inMessages = inMessages;
        this.listeningPort = listeningPort;
    }

    // Run : wait for incoming messages and place them in inMessages
    public void run() {

        int interval = 10;
        long downloadSize = 0;
        long downloadUpdateSize = 0;
        long downloadLogSize = 0;

        long initialTime = System.currentTimeMillis() / 1000;
        long lastMeasure = initialTime;

        try {
            BufferedWriter outBandwidth = null;
            if (OUTPUT) {
                outBandwidth = new BufferedWriter(new FileWriter(selfId
                        + "_downloadBandwidth.txt"));
                outBandwidth.write(selfId + "\n");
            }

            ServerSocketChannel ssc = ServerSocketChannel.open();
            ssc.configureBlocking(false);
            ServerSocket server = ssc.socket();
            server.setReuseAddress(true);

            System.out.println("Node " + selfId + " listens on port "
                    + listeningPort);

            InetSocketAddress localAddr = new InetSocketAddress(listeningPort);
            server.bind(localAddr);

            Selector mySelector = Selector.open();
            ssc.register(mySelector, SelectionKey.OP_ACCEPT);

            while (!mustStop) {
                if (OUTPUT) {
                    int nodeState = peer.getNodeStatus();
                    // Time downloadSize downloadUpdateSize downloadLogSize
                    if (System.currentTimeMillis() / 1000 - lastMeasure >= interval) {
                        lastMeasure = System.currentTimeMillis() / 1000;
                        outBandwidth
                        .write((lastMeasure - initialTime)
                                + " "
                                + nodeState
                                + " "
                                + ((8 * downloadSize) / (1000 * interval))
                                + " "
                                + ((8 * downloadUpdateSize) / (1000 * interval))
                                + " "
                                + ((8 * downloadLogSize) / (1000 * interval))
                                + "\n");
                        downloadSize = 0;
                        downloadUpdateSize = 0;
                        downloadLogSize = 0;
                    }
                }

                if (mySelector.select(1000) == 0) {
                    continue;
                }

                // Get the keys corresponding to the activity
                // that have been detected and process them
                // one by one.
                Set<SelectionKey> keys = mySelector.selectedKeys();
                Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = (SelectionKey) it.next();
                    if (key.isAcceptable()) {
                        // Accept the incoming connection.
                        Socket peer = server.accept();
                        peer.setTcpNoDelay(true);

                        // Make sure to make it nonblocking, so you can
                        // use a Selector on it.
                        SocketChannel sc = peer.getChannel();
                        sc.configureBlocking(false);
                        //peer.setReuseAddress(true);

                        sc.register(mySelector, SelectionKey.OP_READ);
                    } else if (key.isReadable()) {
                        // Read a message
                        SocketChannel sc = (SocketChannel) key.channel();
                        byte[] receiveData = NetUtil.recv(sc);

                        if (receiveData == null) {
                            System.out
                            .println("RECV ERROR: receiveData is null!!!");
                            System.exit(-1);
                        } else {
                            int rs = receiveData.length + Integer.SIZE / 8;

                            ByteArrayInputStream baos = new ByteArrayInputStream(
                                    receiveData);
                            ObjectInputStream oos = new ObjectInputStream(baos);
                            Message receiveMessage = (Message) oos.readObject();

                            if (receiveMessage.messageType != MessageType.NODE_ABSENT
                                    && receiveMessage.messageType != MessageType.EVICTION_NOTIFICATION)
                                downloadSize += rs;

                            if (receiveMessage.messageType == MessageType.EMPTY_UPDATES) {
                                downloadUpdateSize += rs;
                            } else if (receiveMessage.messageType == MessageType.EMPTY_LOG) {
                                downloadLogSize += rs;
                            } else {
                                inMessages.add(receiveMessage);
                            }
                        }
                    }
                    it.remove();
                }
                keys.clear();
            }

            if (OUTPUT)
                outBandwidth.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
