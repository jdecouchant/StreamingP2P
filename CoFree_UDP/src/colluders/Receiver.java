package colluders;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.LinkedBlockingQueue;

import util.Message;
import util.MessageType;


class Receiver extends Thread implements Runnable {

	// Attributes
	private DatagramSocket inSocket;
	private Peer peer;
	private int selfId;
	public LinkedBlockingQueue<Message> inMessages;
	public boolean mustStop;

	private static final boolean OUTPUT = true;

	// Constructor
	public Receiver(int listeningPort, LinkedBlockingQueue<Message> inMessages, int selfId, Peer peer) {
		this.mustStop = false;
		try {
			this.peer = peer;
			this.selfId = selfId;
			this.inMessages = inMessages;
			this.inSocket = new DatagramSocket(listeningPort);
			inSocket.setSoTimeout(10);
		} catch (SocketException e) {
			System.out.println("Peer :  creation of inSocket failed");
			e.printStackTrace();
		}
	}

	// Run : wait for incoming messages and place them in inMessages
	public void run() {

		int interval = 10;
		long downloadSize = 0;
		long downloadUpdateSize = 0;
		long downloadLogSize = 0;

		long initialTime = System.currentTimeMillis()/1000;
		long lastMeasure = initialTime;

		try {
			BufferedWriter outBandwidth = null;
			if (OUTPUT) {
				outBandwidth = new BufferedWriter(new FileWriter(selfId + "_downloadBandwidth.txt"));
				outBandwidth.write(selfId + "\n");
			}
			while (!mustStop) {
				if (OUTPUT) {
					int nodeState = peer.getNodeStatus();
					// Time downloadSize downloadUpdateSize downloadLogSize
					if (System.currentTimeMillis()/1000 - lastMeasure >= interval) {
						lastMeasure = System.currentTimeMillis()/1000;
						outBandwidth.write((lastMeasure - initialTime) 
								+ " " + nodeState 
								+ " "+ ((8*downloadSize)/(1000*interval)) 
								+ " " + ((8*downloadUpdateSize)/(1000*interval))
								+ " " + ((8*downloadLogSize)/(1000*interval)) + "\n");
						downloadSize = 0;
						downloadUpdateSize = 0;
						downloadLogSize = 0;
					}
				}

				try {
					int maxSize = 100000;
					byte[] receiveData = new byte[maxSize];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

					inSocket.receive(receivePacket);

					ByteArrayInputStream baos = new ByteArrayInputStream(receiveData);
					ObjectInputStream oos = new ObjectInputStream(baos);
					Message receiveMessage = (Message)oos.readObject();

					if (receiveMessage.messageType != MessageType.NODE_ABSENT && receiveMessage.messageType != MessageType.EVICTION_NOTIFICATION)
						downloadSize += receivePacket.getLength();

					if (receiveMessage.messageType == MessageType.EMPTY_UPDATES) { 
							downloadUpdateSize += receivePacket.getLength();
					} else if (receiveMessage.messageType == MessageType.EMPTY_LOG)
							downloadLogSize += receivePacket.getLength();
					else 
						inMessages.add(receiveMessage);

				} catch (Exception e) {}

			}

			if (OUTPUT)
				outBandwidth.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
