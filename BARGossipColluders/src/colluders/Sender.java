package colluders;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import util.Message;
import util.MessageType;
import util.Node;

// Sender shares outMessages with main program
// Take data and make a message from it
public class Sender extends Thread implements Runnable {

	public LinkedBlockingQueue<Message> outMessages;
	private ArrayList<Node> nodesList;
	private DatagramSocket outSocket;
	private int updatesLen;
	public boolean mustStop = false;

	private static final boolean OUTPUT = true;
	private int selfId;

	// Constructor
	public Sender(LinkedBlockingQueue<Message> outMessages, int updatesLen, ArrayList<Node> nodesList, int selfId) {
		this.outMessages = outMessages;
		try {
			outSocket = new DatagramSocket();
			outSocket.setSendBufferSize(100000);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		this.nodesList = nodesList;
		this.updatesLen = updatesLen;
		this.selfId = selfId;
	}

	// Find a node in nodeList from its id
	public Node findNode(int nodeId) {
		for (int i = 0; i < nodesList.size(); ++i) {
			if (nodeId == nodesList.get(i).getId())
				return nodesList.get(i);
		}
		return null;
	}

	// Take one out message if it exists and send it
	@Override
	public void run() {
		int interval = 10;
		long uploadSize = 0;
		long uploadUpdateSize = 0;

		long initialTime = System.currentTimeMillis()/1000;
		long lastMeasure = initialTime;

		try {
			BufferedWriter outBandwidth = null;
			if (OUTPUT && selfId != 0) {
				outBandwidth = new BufferedWriter(new FileWriter(selfId + "_uploadBandwidth.txt"));
				outBandwidth.write(selfId + "\n");
			}
			while (!mustStop) {
				if (OUTPUT && selfId != 0) {
					// Time downloadSize downloadUpdateSize downloadLogSize
					if (System.currentTimeMillis()/1000 - lastMeasure >= interval) {
						lastMeasure = System.currentTimeMillis()/1000;
						outBandwidth.write((lastMeasure - initialTime) 
								+ " 2 " + ((8*uploadSize)/(1000*interval)) 
								+ " " + ((8*uploadUpdateSize)/(1000*interval))
								+ " 0\n");
						uploadSize = 0;
						uploadUpdateSize = 0;
					}
				}

				Message sendMessage = outMessages.poll(10, TimeUnit.MILLISECONDS);
				if (sendMessage != null)
				{

					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(sendMessage);
					oos.flush();
					byte[] Buf = baos.toByteArray();

					// 1 - Create useful packet from data and send it
					Node nodeDest = findNode(sendMessage.getReceiverId());
					InetAddress dest = nodeDest.getAddress();
					int destPort = nodeDest.getPort();

					DatagramPacket sendPacket = new DatagramPacket(Buf, Buf.length, dest, destPort);
					outSocket.send(sendPacket);

					uploadSize += sendPacket.getLength();

					// 2 - Create fake packets to consume bandwidth
					// AdditionalSize is in bits originally, we change it to bytes
					int sizeToSend = (int) Math.ceil((sendMessage.getAdditionalSize()) / 8);

					if (sendMessage.getMessageType() == MessageType.BRIEF)
						sizeToSend += sendMessage.getUpdatesIdSize() * updatesLen;

					while (sizeToSend > 0) 
					{
						int msgSize = (64000 < sizeToSend)? 64000: sizeToSend;
						sizeToSend -= msgSize;

						byte[] data = new byte[msgSize];

						Message emptyMsg = new Message(sendMessage.roundId, MessageType.EMPTY_UPDATES, (short) 0, (short) 0, null, null, 0);
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
						uploadUpdateSize += emptyPacket.getLength();
					}
				}
			}

		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}



