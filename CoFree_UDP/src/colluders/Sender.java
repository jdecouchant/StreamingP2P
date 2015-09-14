package colluders;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;

import util.Log;
import util.Message;
import util.MessageType;
import util.Node;

// Sender shares outMessages with main program
// Take data and make a message from it
public class Sender extends Thread implements Runnable {

	private int selfId;
	public LinkedBlockingQueue<Message> outMessages;
	private ArrayList<Node> nodesList;
	private DatagramSocket outSocket;
	private int updatesLen;
	public boolean mustStop;

	private Key publicKey;
	public Peer peer;

	private static final boolean OUTPUT = true;

	// Constructor
	public Sender(int selfId, LinkedBlockingQueue<Message> outMessages, int updatesLen, ArrayList<Node> nodesList) {
		this.selfId = selfId;
		this.mustStop = false;
		this.outMessages = outMessages;
		try {
			outSocket = new DatagramSocket();
			outSocket.setSendBufferSize(100000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
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
	}

	public Sender(int selfId, LinkedBlockingQueue<Message> outMessages, int updatesLen, ArrayList<Node> nodesList, Peer peer) {
		this(selfId, outMessages, updatesLen, nodesList);
		this.peer = peer;
	}

	public void computeOneAuthenticator() {
		try {
			byte[] data = new byte[Log.HASH_SIZE + Log.LOG_ENTRY_ID_SIZE];
			new Random().nextBytes(data);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey); // initialize object's mode and key
			cipher.doFinal(data); // use object for encryption	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Take one out message if it exists and send it
	public void run() 
	{
		int interval = 10;
		long uploadSize = 0;
		long uploadUpdateSize = 0;
		long uploadLogSize = 0;

		long initialTime = System.currentTimeMillis()/1000;
		long lastMeasure = initialTime;

		try {
			BufferedWriter outBandwidth = null;
			if (OUTPUT && selfId != 0) {
				outBandwidth = new BufferedWriter(new FileWriter(selfId + "_uploadBandwidth.txt"));
				outBandwidth.write(selfId + "\n");
			}

			while (!mustStop)
			{
				if (OUTPUT && selfId != 0) {
					int nodeState = peer.getNodeStatus();
					// Time downloadSize downloadUpdateSize downloadLogSize
					if (System.currentTimeMillis()/1000 - lastMeasure >= interval) {
						lastMeasure = System.currentTimeMillis()/1000;
						outBandwidth.write((lastMeasure - initialTime) 
								+ " " + nodeState 
								+ " "+ ((8*uploadSize)/(1000*interval)) 
								+ " " + ((8*uploadUpdateSize)/(1000*interval))
								+ " " + ((8*uploadLogSize)/(1000*interval)) + "\n");
						uploadSize = 0;
						uploadUpdateSize = 0;
						uploadLogSize = 0;
					}
				}
				
				if (8*uploadSize/(1000*(System.currentTimeMillis()-lastMeasure)) < 1000) { // Limit bandwidth at 1000 kbps
					Message sendMessage = outMessages.poll(10, TimeUnit.MILLISECONDS);
					if (sendMessage != null) {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ObjectOutputStream oos = new ObjectOutputStream(baos);
						oos.writeObject(sendMessage);
						oos.flush();
						byte[] Buf = baos.toByteArray();

						// 1 - Create useful packet from data and send it
						Node nodeDest = nodesList.get(sendMessage.receiverId);
						InetAddress dest = nodeDest.address;
						int destPort = nodeDest.port;
						if (sendMessage.messageType == MessageType.SERVE || sendMessage.messageType == MessageType.PROPOSE
								|| sendMessage.messageType == MessageType.REQUEST)
							computeOneAuthenticator();

						DatagramPacket sendPacket = new DatagramPacket(Buf, Buf.length, dest, destPort);
						outSocket.send(sendPacket);

						if (sendMessage.messageType != MessageType.NODE_ABSENT && sendMessage.messageType != MessageType.EVICTION_NOTIFICATION)
							uploadSize += sendPacket.getLength();

						// 2 - Create fake packets to consume bandwidth
						// AdditionalSize is originally in bits, we change it to bytes
						int sizeToSend = (int) Math.ceil((double) (sendMessage.additionalSize) / (double) 8);
						
						if (sendMessage.messageType == MessageType.PROPOSE || sendMessage.messageType == MessageType.REQUEST ||
								sendMessage.messageType == MessageType.SERVE || sendMessage.messageType == MessageType.AUDIT ||
								sendMessage.messageType == MessageType.AUDIT_RESP)
							sizeToSend += (int) Math.ceil((double) Log.AUTHENTICATOR_SIZE / (double) 8);

						// TODO: is it necessary to send more authenticators?

						while (sizeToSend > 0) 
						{
							int msgSize = (64000 < sizeToSend)? 64000: sizeToSend;
							sizeToSend -= msgSize;

							byte[] data = new byte[msgSize];

							Message emptyMsg = new Message(sendMessage.roundId, MessageType.EMPTY_LOG, (short) 0, (short) 0);
							ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
							ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
							oos2.writeObject(emptyMsg);
							oos2.flush();

							byte[] tmp = baos2.toByteArray();

							if (tmp.length > data.length)
								data = new byte[tmp.length];

							System.arraycopy(tmp, 0, data, 0, tmp.length);

							DatagramPacket emptyPacket = new DatagramPacket(data, data.length, dest, destPort);

							outSocket.send(emptyPacket);

							uploadSize += emptyPacket.getLength();
							uploadLogSize += emptyPacket.getLength();
						}

						sizeToSend = 0;
						if (sendMessage.messageType == MessageType.SERVE) {
							sizeToSend += sendMessage.getUpdatesIdSize() * updatesLen;
						}


						while (sizeToSend > 0) 
						{
							int msgSize = (64000 < sizeToSend)? 64000: sizeToSend;
							sizeToSend -= msgSize;

							byte[] data = new byte[msgSize];

							Message emptyMsg = new Message(sendMessage.roundId, MessageType.EMPTY_UPDATES, (short) 0, (short) 0);
							ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
							ObjectOutputStream oos2 = new ObjectOutputStream(baos2);
							oos2.writeObject(emptyMsg);
							oos2.flush();

							byte[] tmp = baos2.toByteArray();

							if (tmp.length > data.length)
								data = new byte[tmp.length];

							System.arraycopy(tmp, 0, data, 0, tmp.length);

							DatagramPacket emptyPacket = new DatagramPacket(data, data.length, dest, destPort);

							uploadSize += emptyPacket.getLength();
							uploadUpdateSize += emptyPacket.getLength();

							outSocket.send(emptyPacket);
						}
					}
				}
			}

			if (OUTPUT && selfId != 0)
				outBandwidth.close();

		} catch(Exception e) 
		{
			e.printStackTrace();
		}
	}
}
