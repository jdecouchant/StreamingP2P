package colluders;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import util.MessageType;
import util.Message;
import util.Node;
import colluders.Sender;

public class Server {

	// Attributes
	private short selfId; // The server id is equal to 0
	private int roundId;

	// Membership
	private Set<Short> epochAbsentNodes; // Nodes that left the session since the beginning of new epoch
	private Set<Short> epochList, epochListPlus1, epochListPlus2;

	private Set<Short> nodesThatJustLeft;
	private Set<Short> nodesThatReceivedEpochListPlus1;

	// Protocol constants
	private final int FANOUT, UPDATES_NBR_PER_ROUND, EPOCH;
	public static final int UPDATES_NBR_PER_SERVE = 1;

	public static final int NB_SENDER = 5;
	public int senderId = 0;

	public Sender senders[];
	public Thread sendersTh[];
	public ArrayList<LinkedBlockingQueue<Message>> outMessages;

	public Thread receiverTh;
	public LinkedBlockingQueue<Message> inMessages;

	private int nbServeRounds; // Keep track of the number of rounds in which the server sent updates
	private final Random generator;

	// Used to tell a node that it must terminate
	public boolean mustStop = false;

	private static final boolean DEBUG = false;

	// Constructor
	Server(int listeningPort, ArrayList<Node> nodesList, int FANOUT, int UPDATES_NBR_PER_ROUND, int updatesLen, int EPOCH) {
		this.selfId = 0;
		this.roundId = 1;

		this.epochList = new HashSet<Short>();
		this.epochListPlus1 = new HashSet<Short>();
		this.epochListPlus2 = new HashSet<Short>();
		this.epochAbsentNodes = new HashSet<Short>();

		this.nodesThatJustLeft = new HashSet<Short>();
		this.nodesThatReceivedEpochListPlus1 = new HashSet<Short>();

		this.FANOUT = FANOUT;
		this.EPOCH = EPOCH;

		// Create receiver : receive messages, extract useful data and put it into inMessages
		this.inMessages = new LinkedBlockingQueue<Message>();
		this.receiverTh = new Thread(new Receiver(inMessages, listeningPort));
		this.receiverTh.start();

		// Create sender : from useful data in outMessages, make a packet and send it

		senders = new Sender[NB_SENDER];
		sendersTh = new Thread[NB_SENDER];
		outMessages = new ArrayList<LinkedBlockingQueue<Message>>();
		for (int i = 0; i < NB_SENDER; ++i) {
			outMessages.add(new LinkedBlockingQueue<Message>());
			senders[i] = new Sender(0, outMessages.get(i), updatesLen, nodesList);
			sendersTh[i] = new Thread(senders[i]);
			sendersTh[i].start();
		}

		// Statistics
		this.UPDATES_NBR_PER_ROUND = UPDATES_NBR_PER_ROUND;
		nbServeRounds = 0;

		this.generator = new Random(System.currentTimeMillis());
	}

	public void stop() {
		this.mustStop = true;
		for (int i = 0; i < NB_SENDER; ++i)
			senders[i].mustStop = true;
	}

	public void newRound() {
		System.out.println("Server : round " + roundId);

		boolean epochOrFirstRound = (roundId % EPOCH == 0 || roundId == 1);
		if (epochOrFirstRound) {
			if (DEBUG)
				System.out.println("\tNew EPOCH");

			nodesThatReceivedEpochListPlus1.clear();

			// epochList U= epochListPlus1
			epochList.addAll(epochListPlus1);

			epochListPlus1 = new HashSet<Short>(); // epochListPlus1 = epochList U
			epochListPlus1.addAll(epochListPlus2);

			epochListPlus2 = new HashSet<Short>();
			epochAbsentNodes.clear();

			if (DEBUG) {
				System.out.println("Epoch list : " + epochList.toString());
				System.out.println("Epoch list + 1: " + epochListPlus1.toString());
				System.out.println("New nodes + 2: " + epochListPlus2.toString());
			}
		}

		epochAbsentNodes.addAll(nodesThatJustLeft);
		epochList.removeAll(nodesThatJustLeft);
		epochListPlus1.removeAll(nodesThatJustLeft);
		epochListPlus2.removeAll(nodesThatJustLeft);
		nodesThatJustLeft.clear();

		ArrayList<Short> selectedClients = new ArrayList<Short>();
		ArrayList<Short> availableClients = new ArrayList<Short>();

		if (epochList.size() > 0)
			availableClients.addAll(epochList);
		else if (epochListPlus1.size() > 0){
			availableClients.addAll(epochListPlus1);
		} else {
			availableClients.addAll(epochListPlus2);
		}

		Short epochAbsentNodesArray[] = epochAbsentNodes.toArray(new Short[epochAbsentNodes.size()]);
		Short epochListPlus1Array[] = epochListPlus1.toArray(new Short[epochListPlus1.size()]);

		int availableIndex;
		short toClientId;
		ArrayList<Integer> updates = new ArrayList<Integer>();
		Message sendMessage;

		if (availableClients.size() > 0) {
			int updateValue = UPDATES_NBR_PER_ROUND*(roundId-1) + 1;
			for (int firstUpdateId = 0; firstUpdateId < UPDATES_NBR_PER_ROUND; firstUpdateId+=UPDATES_NBR_PER_SERVE) {

				int firstUpdateValue = updateValue + firstUpdateId;
				for (int i = 0; i < UPDATES_NBR_PER_SERVE; ++i) {
					updates.add(i, firstUpdateValue);
					++firstUpdateValue;
				}

				for (int clientIndex=0; clientIndex < Math.min(FANOUT, availableClients.size()); ++clientIndex) 	{
					availableIndex = generator.nextInt(availableClients.size());
					toClientId =  availableClients.get(availableIndex);
					
					if (DEBUG)
						System.out.println("Sending updates to " + toClientId);

					selectedClients.add(toClientId);
					availableClients.remove(availableIndex);

					if (epochOrFirstRound && !nodesThatReceivedEpochListPlus1.contains(toClientId)) {
						if (DEBUG)
							System.out.println("\tSend epochList+1 to node " + toClientId);
						nodesThatReceivedEpochListPlus1.add(toClientId);
						sendMessage = new Message(roundId, MessageType.SERVE, selfId, toClientId);
						sendMessage.setNodes1(epochListPlus1Array);
					} else {				
						sendMessage = new Message(roundId, MessageType.SERVE, selfId, toClientId);
					}
					sendMessage.setNodes2(epochAbsentNodesArray);
					sendMessage.setUpdatesId(updates);

					outMessages.get(senderId).add(sendMessage);
					++senderId;
					senderId %= NB_SENDER;
				}
				availableClients.addAll(selectedClients);
				selectedClients.clear();
				updates.clear();
			}
		}

		++nbServeRounds;
		++this.roundId;
	}

	public int getNbServeRounds() {
		return nbServeRounds;
	}

	public void receiveJOIN_MESSAGE(Message message) {
		epochListPlus2.add(message.senderId);
		if (DEBUG)
			System.out.println("\tNode " + message.senderId + " is waiting to join the session");
		sendJOIN_MESSAGE_RESP(message.senderId);
	}

	public void sendJOIN_MESSAGE_RESP(short toPartnerId) {
		ArrayList<Short> nodesPresentOrWaiting = new ArrayList<Short>();

		nodesPresentOrWaiting.addAll(epochList);
		nodesPresentOrWaiting.addAll(epochListPlus1);
		nodesPresentOrWaiting.addAll(epochListPlus2);

		Message sendMessage = new Message(roundId, MessageType.JOIN_SESSION_RESP, selfId, toPartnerId);
		sendMessage.setAdditionalSize(epochList.size());
		sendMessage.setNodes1(nodesPresentOrWaiting);

		outMessages.get(senderId).add(sendMessage);
		++senderId;
		senderId %= NB_SENDER;
	}

	public void receiveQUIT_MESSAGE(Message message) {
		short idAbsentNode = message.senderId;
		nodesThatJustLeft.add(idAbsentNode);
		epochAbsentNodes.add(idAbsentNode);
		if (DEBUG)
			System.out.println("Node " + idAbsentNode + " left the session");
	}

	public void receivePOM_MESSAGE(Message message) { // Currently, an incorrect node is removed from all lists, and added to the list of absent nodes
		if (DEBUG)
			System.out.println("\n\nNode "+message.senderId+" is incorrect!\n\n");
		short incorrectNodeId = message.senderId;
		nodesThatJustLeft.add(incorrectNodeId);
		epochAbsentNodes.add(incorrectNodeId);
		epochList.remove(incorrectNodeId);
		epochListPlus1.remove(incorrectNodeId);
		epochListPlus2.remove(incorrectNodeId);

		Message sendMessage = new Message(roundId, MessageType.EVICTION_NOTIFICATION, selfId, incorrectNodeId);

		outMessages.get(senderId).add(sendMessage);
		++senderId;
		senderId %= NB_SENDER;
	}

	public void treatInMessage() {
		try {
			Message message = inMessages.poll(10, TimeUnit.MILLISECONDS);
			if (message != null) // Nodes can treat in messages if it joined the session
			{	
				switch (message.messageType) {
				case MessageType.JOIN_SESSION:
					receiveJOIN_MESSAGE(message);
					break;
				case MessageType.QUIT_SESSION:
					receiveQUIT_MESSAGE(message);
					break;
				case MessageType.POM:
					receivePOM_MESSAGE(message);
					break;
				}	
			}
		} catch (Exception e) {}
	}

	/***************************************/
	/**  RECEIVER THREAD                  **/
	/***************************************/	

	// Receiver shares inMessages with main program
	// Wait for incoming messages, extract data and place it in inMessages
	private class Receiver extends Thread implements Runnable {

		// Attributes
		private DatagramSocket inSocket;
		public LinkedBlockingQueue<Message> inMessages;

		// Constructor
		public Receiver(LinkedBlockingQueue<Message> inMessages, int listeningPort) {
			this.inMessages = inMessages;
			try {
				this.inSocket = new DatagramSocket(listeningPort);
				inSocket.setSoTimeout(100);
			} catch (SocketException e) {
				System.out.println("Peer :  creation of inSocket failed");
				e.printStackTrace();
			}
			//			System.out.println("Receiver 0: " + ManagementFactory.getRuntimeMXBean().getName());
		}

		// Run : wait for incoming messages and place them in inMessages
		public void run() {

			while (!mustStop) {
				try {
					int maxSize = 100000;
					byte[] receiveData = new byte[maxSize];
					DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

					inSocket.receive(receivePacket);

					ByteArrayInputStream baos = new ByteArrayInputStream(receiveData);
					ObjectInputStream oos = new ObjectInputStream(baos);
					Message receiveMessage = (Message)oos.readObject();

					inMessages.add(receiveMessage);
				} catch (Exception e) {}
			}
		}

	}

}
