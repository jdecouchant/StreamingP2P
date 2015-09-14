package colluders;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import util.BufferMap;
import util.Message;
import util.MessageType;
import util.Node;

public class Peer 
{
	private static final int HASH_SIZE = 160; // SHA-1
	public static final int SIGN_SIZE = 1024; // RSA 1024

	// Attributes of peer
	private final short selfId;

	private final boolean selfIsColluder;
	private ArrayList<Short> colluderPartners;

	private int roundId;

	// Global membership
	private final int nbNodes;

	// Owned updates and logs
	public BufferMap BM;

	// Protocol parameters
	private final int updatesNbrPerRound;
	private final int updatesLen;

	private final int RTE;
	private final int BAL_SIZE;
	private final int PUSH_SIZE;
	private final int PUSH_AGE;

	// Thread and communication queue
	public Sender sender;
	public Thread senderTh, receiverTh;
	public LinkedBlockingQueue<Message> inMessages, outMessages;

	private long nbrReceivedUpdates;
	public boolean mustStop = false;

	// Constructor
	Peer(short selfId, int listeningPort, ArrayList<Node> nodesList, int GROUP_SIZE, int GROUP_NBR, int RTE, 
			int BAL_SIZE, int PUSH_SIZE, int PUSH_AGE) {

		this.selfId = selfId;
		this.selfIsColluder = selfId < (GROUP_SIZE * GROUP_NBR);
		if (selfIsColluder)	{
			this.colluderPartners = new ArrayList<Short>();
			for (short partnerId = (short) (GROUP_SIZE * (selfId / GROUP_SIZE)); partnerId < (short) (GROUP_SIZE * (selfId/GROUP_SIZE + 1)); ++partnerId) {
				if (partnerId != selfId)
					colluderPartners.add(partnerId);
			}
		}

		this.RTE = RTE;
		this.BAL_SIZE = BAL_SIZE;
		this.PUSH_SIZE = PUSH_SIZE;
		this.PUSH_AGE = PUSH_AGE;

		this.nbNodes = nodesList.size();

		this.roundId = 1;

		this.updatesNbrPerRound = Colluders.UPD_NB_PER_ROUND;
		this.updatesLen = Colluders.UPD_LEN;
		this.BM = new BufferMap(RTE, updatesNbrPerRound);

		// Create receiver : receive messages, extract useful data and put it into inMessages
		this.inMessages = new LinkedBlockingQueue<Message>();
		this.receiverTh = new Thread(new Receiver(listeningPort));
		this.receiverTh.start();

		// Create sender : from useful data in outMessages, make a packet and send it
		this.outMessages = new LinkedBlockingQueue<Message>();
		this.sender = new Sender(this.outMessages, updatesLen, nodesList, selfId);
		this.senderTh = new Thread(sender);
		this.senderTh.start();

		// Statistics
		this.nbrReceivedUpdates = 0;
	}

	public int getRoundId() {
		return this.roundId;
	}

	// Methods
	// Used to collect final statistics
	public long getNbrReceivedUpdates() 
	{
		return nbrReceivedUpdates;
	}

	public void newRound() 
	{
		System.out.println("\nPeer " + selfId + ": round " + roundId);

		// Find new partners, and initiate associations with them

		Random generator = new Random(System.currentTimeMillis());

		short toPartnerIdBal = (short) generator.nextInt(nbNodes);
		while (toPartnerIdBal == selfId)
			toPartnerIdBal = (short) generator.nextInt(nbNodes);

		sendBAL(toPartnerIdBal);

		if (! selfIsColluder) {
			short toPartnerIdPush = (short) generator.nextInt(nbNodes);
			while (toPartnerIdPush == toPartnerIdBal || toPartnerIdPush == selfId)
				toPartnerIdPush = (short) generator.nextInt(nbNodes);
			sendPUSH(toPartnerIdPush);
		}
	}

	public int endOfRound() {
		int deletedUpdNbr = BM.actualize(roundId);
		++this.roundId;	
		return deletedUpdNbr;
	}

	public void treatInMessage() {
		try {
			Message message = inMessages.poll(10, TimeUnit.MILLISECONDS);
			if (message != null) {	
				switch (message.getMessageType()) 
				{
				case MessageType.BAL:
					receiveBAL(message);
					break;
				case MessageType.BAL_RESP:
					receiveBAL_RESP(message);
					break;
				case MessageType.PUSH:
					receivePUSH(message);
					break;
				case MessageType.PUSH_RESP:
					receivePUSH_RESP(message);
					break;
				case MessageType.DIVULGE:
					receiveDIVULGE(message);
					break;
				case MessageType.BRIEF:
					receiveBRIEF(message);
					break;
				case MessageType.KEY_RQST:
					receiveKEY_RQST(message);
					break;
				case MessageType.KEY_RESP:
					receiveKEY_RESP(message);
					break;
				case MessageType.COLLUDERS:
					receiveCOLLUDERS(message);
					break;
				}
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void receiveCOLLUDERS(Message msg) {
		for (int i = 0; i < msg.getUpdatesIdSize(); ++i)
			BM.insertUpdate(roundId, msg.getUpdatesId(i));
	}

	private void sendBAL(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.BAL, selfId, toPartnerId, null, null, HASH_SIZE + SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receiveBAL(Message msg) {
		sendBAL_RESP(msg.getSenderId());
	}

	private void sendBAL_RESP(short toPartnerId) {	
		Message sendMessage = new Message(roundId, MessageType.BAL_RESP, selfId, toPartnerId, BM.getUpdates(roundId), null, SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receiveBAL_RESP(Message message) {

		sendDIVULGE(message.getSenderId());

		// compute what can be sent
		ArrayList<Integer> senderUpdates = new ArrayList<Integer>();
		ArrayList<Integer> canReceive = new ArrayList<Integer>();
		for (int i = 0; i < message.getUpdatesIdSize(); ++i) {
			senderUpdates.add(message.getUpdatesId(i));
			canReceive.add(message.getUpdatesId(i));
		}

		// Contains the updates we have the partner does not
		ArrayList<Integer> ownUpdates = new ArrayList<Integer>();
		ArrayList<Integer> canSend = new ArrayList<Integer>();
		for (int i = 0; i < BM.getBMSize(); ++i) {
			canSend.add(BM.getUpdatesId(i));
			ownUpdates.add(BM.getUpdatesId(i));
		}

		canSend.removeAll(senderUpdates);
		canReceive.removeAll(ownUpdates);

		int nbrToSend = Math.min(canSend.size(), canReceive.size());
		nbrToSend = Math.min(nbrToSend, BAL_SIZE);

		Collections.sort(canSend); // to send preferently the oldest updates
		while (canSend.size() > nbrToSend)
			canSend.remove(canSend.size()-1);

		sendBRIEF(message.getSenderId(), canSend, 0);
	}

	private void sendDIVULGE(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.DIVULGE, selfId, toPartnerId, BM.getUpdates(roundId), null, SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receiveDIVULGE(Message message) {		
		// compute what can be sent
		ArrayList<Integer> senderUpdates = new ArrayList<Integer>();
		ArrayList<Integer> canReceive = new ArrayList<Integer>();
		for (int i = 0; i < message.getUpdatesIdSize(); ++i) {
			senderUpdates.add(message.getUpdatesId(i));
			canReceive.add(message.getUpdatesId(i));
		}

		// Contains the updates we have the partner does not
		ArrayList<Integer> ownUpdates = new ArrayList<Integer>();
		ArrayList<Integer> canSend = new ArrayList<Integer>();
		for (int i = 0; i < BM.getBMSize(); ++i) {
			canSend.add(BM.getUpdatesId(i));
			ownUpdates.add(BM.getUpdatesId(i));
		}

		canSend.removeAll(senderUpdates);
		canReceive.removeAll(ownUpdates);

		int nbrToSend = Math.min(canSend.size(), canReceive.size());
		nbrToSend = Math.min(nbrToSend,  BAL_SIZE);

		Collections.sort(canSend);
		while (canSend.size() > nbrToSend)
			canSend.remove(canSend.size()-1);

		sendBRIEF(message.getSenderId(), canSend, 0);
	}

	// This function is used in sendPUSH to find the updates released in the last pushAge rounds
	int fromRound(int updateId) {
		return updateId / updatesNbrPerRound;
	}

	private void sendPUSH(short toPartnerId)
	{
		// contains updates Id emitted in the last pushAge rounds
		ArrayList<Integer> youngList = new ArrayList<Integer>();
		for (int updatePos = 0; updatePos < BM.getBMSize(); ++updatePos) {
			if (fromRound(BM.getUpdatesId(updatePos)) >= roundId - PUSH_AGE)
				youngList.add(BM.getUpdatesId(updatePos));
		}
		Collections.sort(youngList);

		// Find old updates that sender is missing that will expire in upcoming rounds
		ArrayList<Integer> oldList = new ArrayList<Integer>();
		int limInf = (roundId + 1 - RTE) * updatesNbrPerRound;
		int limSup = limInf + 5*updatesNbrPerRound;
		for (int updateId = Math.max(limInf,0); updateId < limSup; ++updateId) 
		{
			if (! BM.isInserted(updateId))
				oldList.add(updateId);
		}
		Collections.sort(oldList);

		Message sendMessage = new Message(roundId, MessageType.PUSH, selfId, toPartnerId, youngList, oldList, SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receivePUSH(Message message) 
	{
		if (! selfIsColluder)
		{
			// Select wantList in youngUpdates
			ArrayList<Integer> wantList = new ArrayList<Integer>();
			for (int i = 0; i < message.getUpdatesIdSize() && wantList.size() < PUSH_SIZE; ++i) 
			{
				if (! BM.isInserted(message.getUpdatesId(i)))
					wantList.add(message.getUpdatesId(i));
			}

			sendPUSH_RESP(message.getSenderId(), wantList);

			// Select oldUpdates that can be sent to partner
			ArrayList<Integer> updatesInOldList = new ArrayList<Integer>();
			for (int i = 0; i < message.getOldListSize() && updatesInOldList.size() < PUSH_SIZE; ++i) 
			{
				if (BM.isInserted(message.getOldListId(i)))
					updatesInOldList.add(message.getOldListId(i));
			}
			Collections.sort(updatesInOldList);

			// junk updates have to be taken in consideration
			int junkToSend = 0;
			if (wantList.size() - updatesInOldList.size() > 0)
				junkToSend += wantList.size() - updatesInOldList.size();

			sendBRIEF(message.getSenderId(), updatesInOldList, junkToSend);
		}
	}

	private void sendPUSH_RESP(short toPartnerId, ArrayList<Integer> wantList) {
		Message sendMessage = new Message(roundId, MessageType.PUSH_RESP, selfId, toPartnerId, wantList, null, SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receivePUSH_RESP(Message message) {
		ArrayList<Integer> wantList = new ArrayList<Integer>();
		for (int i = 0; i < message.getUpdatesIdSize() && wantList.size() < PUSH_SIZE; ++i) {
			if (! BM.isInserted(message.getUpdatesId(i)))
				wantList.add(message.getUpdatesId(i));
		}
		sendBRIEF(message.getSenderId(), wantList, 0);
	}

	private void sendBRIEF(short toPartnerId, ArrayList<Integer> updList, int junkToSend) {
		Message sendMessage = new Message(roundId, MessageType.BRIEF, selfId, toPartnerId, updList, null, SIGN_SIZE + 1.27*junkToSend*updatesLen*8);
		outMessages.add(sendMessage);
	}

	private void receiveBRIEF(Message message) {
		for (int i = 0; i < message.getUpdatesIdSize(); ++i)
			BM.insertUpdate(roundId, message.getUpdatesId(i));
		if (message.getSenderId() != 0)	
			sendKEY_RQST(message.getSenderId());

		if (selfIsColluder) {
			for (int partnerPos = 0; partnerPos < colluderPartners.size(); ++partnerPos) {
				Message sendMessage = new Message(roundId, MessageType.COLLUDERS, selfId, colluderPartners.get(partnerPos), BM.getUpdates(roundId), null, 0);
				outMessages.add(sendMessage);				
			}
		}
	}

	/* Key Exchange */
	private void sendKEY_RQST(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.KEY_RQST, selfId, toPartnerId, null, null, SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receiveKEY_RQST(Message message) {
		sendKEY_RESP(message.getSenderId());
	}


	private void sendKEY_RESP(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.KEY_RESP, selfId, toPartnerId, null, null, 2*SIGN_SIZE);
		outMessages.add(sendMessage);
	}

	private void receiveKEY_RESP(Message message) {
		// Do nothing
	}

	/***************************************/
	/**  RECEIVER THREAD                  **/
	/***************************************/	

	// Receiver shares inMessages with main program
	// Wait for incoming messages, extract data and place it in inMessages
	private class Receiver extends Thread implements Runnable {

		// Attributes
		private DatagramSocket inSocket;

		// Constructor
		public Receiver(int listeningPort) 
		{
			try {
				this.inSocket = new DatagramSocket(listeningPort);
				inSocket.setSoTimeout(10);
			} catch (SocketException e) {
				System.out.println("Peer :  creation of inSocket failed");
				e.printStackTrace();
			}
		}

		// Run : wait for incoming messages and place them in inMessages
		@Override
		public void run() 
		{
			int interval = 10;
			long downloadSize = 0;
			long downloadUpdateSize = 0;

			long initialTime = System.currentTimeMillis()/1000;
			long lastMeasure = initialTime;

			try {
				BufferedWriter outBandwidth = new BufferedWriter(new FileWriter(selfId + "_downloadBandwidth.txt"));

				while (!mustStop) 
				{
					if (System.currentTimeMillis()/1000 - lastMeasure >= interval)
					{
						try
						{
							lastMeasure = System.currentTimeMillis()/1000;
							outBandwidth.write((lastMeasure - initialTime) + " 2 " + ((8*downloadSize)/(1000*interval)) 
									+ " " + ((8*downloadUpdateSize)/(1000*interval))
									+ " 0\n");
							downloadSize = 0;
							downloadUpdateSize = 0;
						} catch (Exception e) {}
					}

					try 
					{
						int maxSize = 100000;
						byte[] receiveData = new byte[maxSize];
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

						inSocket.receive(receivePacket);

						ByteArrayInputStream baos = new ByteArrayInputStream(receiveData);
						ObjectInputStream oos = new ObjectInputStream(baos);
						Message receiveMessage = (Message)oos.readObject();

						if (receiveMessage.messageType != MessageType.COLLUDERS) {
							downloadSize += receivePacket.getLength();
						}

						if (receiveMessage.messageType == MessageType.EMPTY_UPDATES) {
								downloadUpdateSize += receivePacket.getLength();
						} else 
							inMessages.add(receiveMessage);
					} catch (Exception e) 
					{ }
				}

				outBandwidth.close();

			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

	}

}


