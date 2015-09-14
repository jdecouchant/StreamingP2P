package colluders;

import java.io.ByteArrayOutputStream;
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

import javax.crypto.Cipher;

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

	public Key publicKey;

	// Constructor
	public Sender(LinkedBlockingQueue<Message> outMessages, int updatesLen, ArrayList<Node> nodesList) 
	{
		this.outMessages = outMessages;
		try 
		{
			outSocket = new DatagramSocket();
			outSocket.setSendBufferSize(100000);
		} catch (SocketException e) 
		{
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

	public void signMessage(int sizeToSign)
	{
		try 
		{
			byte[] data = new byte[sizeToSign];
			new Random().nextBytes(data);
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey); // initialize object's mode and key
			cipher.doFinal(data); // use object for encryption	
		} catch (Exception e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	public static final void decryptMessage(int sizeToSign, Key publicKey)
	{
		if (sizeToSign > 0)
		{
			try 
			{
				byte[] data = new byte[sizeToSign];
				new Random().nextBytes(data);
				Cipher cipher = Cipher.getInstance("RSA");
				cipher.init(Cipher.DECRYPT_MODE, publicKey); // initialize object's mode and key
				cipher.doFinal(data); // use object for encryption	
			} catch (Exception e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}

	// Find a node in nodeList from its id
	public Node findNode(int nodeId)
	{
		for (int i = 0; i < nodesList.size(); i++)
		{
			if (nodeId == nodesList.get(i).getId())
				return nodesList.get(i);
		}
		return null;
	}

	// Take one out message if it exists and send it
	public void run() 
	{
		while (!mustStop)
		{
			Message sendMessage = outMessages.poll();
			if (sendMessage != null)
			{
				try {
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

					// 2 - Create fake packets to consume bandwidth
					// AdditionalSize is in bits originally, we change it to bytes
					int sizeToSend = (int) Math.ceil((double) (sendMessage.getAdditionalSize()) / (double) 8);

					if (sendMessage.getMessageType() == MessageType.BRIEF)
						sizeToSend += sendMessage.getUpdatesIdSize() * updatesLen;

					while (sizeToSend > 0) 
					{
						int msgSize = (64000 < sizeToSend)? 64000: sizeToSend;
						sizeToSend -= msgSize;

						byte[] data = new byte[msgSize];

						int toEncrypt = msgSize;
						while (toEncrypt > 0)
						{
							signMessage(245);
							toEncrypt -= 245;
						}

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
					}

				} catch(Exception e) 
				{
					e.printStackTrace();
				}
			}
		}
	}



}