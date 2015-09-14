package colluders;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import util.MessageType;
import util.Node;
import util.Message;
import colluders.Sender;

public class Server 
{
	// Attributes

	private short selfId; // = 0
	private int roundId;

	// Global membership
	private int nodesNbr;
	private ArrayList<Short> idNodes;

	// Partners of peer
	private final int FANOUT;
	private final int UPD_NB_PER_ROUND;

	private static final int NB_SENDER = 5;
	private int senderId;
	public Sender sender[];
	public Thread senderTh[];
	public ArrayList<LinkedBlockingQueue<Message>> outMessages;

	private int nbrSentUpdates;
	
	private Random generator;
	
	// Used to tell a node that it must terminate
	public boolean mustStop = false;

	// Constructor
	Server(ArrayList<Node> nodesList, int FANOUT) {
		this.selfId = 0;
		this.roundId = 1;

		this.nodesNbr = nodesList.size();
		this.idNodes = new ArrayList<Short>();
		for (short i = 1; i < nodesNbr; ++i) {
			if (i==selfId)
				continue;
			idNodes.add(i);
		}

		this.FANOUT = FANOUT;

		// Create sender : from useful data in outMessages, make a packet and send it
		this.senderId = 0;
		outMessages = new ArrayList<LinkedBlockingQueue<Message>>();
		sender = new Sender[NB_SENDER];
		senderTh = new Thread[NB_SENDER];
		for (int i = 0; i < NB_SENDER; ++i) {
			outMessages.add(i, new LinkedBlockingQueue<Message>());
			sender[i] = new Sender(outMessages.get(i), Colluders.UPD_LEN, nodesList, selfId);
			senderTh[i] = new Thread(sender[i]);
			senderTh[i].start();
		}
		
		this.generator = new Random(System.currentTimeMillis());
		
		// Statistics
		this.UPD_NB_PER_ROUND = Colluders.UPD_NB_PER_ROUND;
		nbrSentUpdates = 0;
	}

	public void stop() {
		this.mustStop = true;
		for (int i = 0; i < NB_SENDER; ++i) {
			this.sender[i].mustStop = true;
		}
	}
	
	public void newRound() {
		System.out.println("Server : round " + roundId);
		
		ArrayList<Short> availableClients = new ArrayList<Short>();
		availableClients.addAll(idNodes);
		
		short toClientPos, toClientId;
		int updateId;
		for (int updatePos = 0; updatePos < UPD_NB_PER_ROUND; ++updatePos) {
			ArrayList<Integer> updates = new ArrayList<Integer>();
			updateId = UPD_NB_PER_ROUND*(roundId-1) + 1 + updatePos;
			updates.add(updateId);
			
			for (int clientIndex=0; clientIndex < Math.min(availableClients.size(), FANOUT); ++clientIndex) {
				
				toClientPos = (short) generator.nextInt(availableClients.size());
				toClientId = availableClients.get(toClientPos);
				
				availableClients.remove(toClientPos);

				Message sendMessage = new Message(roundId, MessageType.BRIEF, selfId, toClientId, updates, null, 0);
				outMessages.get(senderId).add(sendMessage);
				++senderId;
				senderId %= NB_SENDER;
			}
			availableClients.addAll(idNodes);
		}
		
		
		++nbrSentUpdates;
		++this.roundId;
	}

	public int getNbrSentUpdates() {
		return nbrSentUpdates;
	}
	
}
