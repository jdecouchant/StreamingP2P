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

	// Partners of peer
	private int fanout;
	private int updatesNbrPerRound;

	public Sender sender;
	public Thread senderTh;
	public LinkedBlockingQueue<Message> outMessages;

	private int nbrSentUpdates;

	// Used to tell a node that it must terminate
	public boolean mustStop = false;

	// Constructor
	Server(ArrayList<Node> nodesList, int updatesNbrPerRound, int updatesLen, int fanout) 
	{
		this.selfId = 0;
		this.roundId = 1;

		this.nodesNbr = nodesList.size();

		this.fanout = fanout;

		// Create sender : from useful data in outMessages, make a packet and send it
		outMessages = new LinkedBlockingQueue<Message>();
		sender = new Sender(outMessages, updatesLen, nodesList);
		this.senderTh = new Thread(sender);
		senderTh.start();

		// Statistics
		this.updatesNbrPerRound = updatesNbrPerRound;
		nbrSentUpdates = 0;
	}

	public void newRound() 
	{
		System.out.println("Server : round " + roundId);

		Random generator = new Random(System.currentTimeMillis());
		short clientsId[] = new short[fanout];


		for (int iter = 0; iter < updatesNbrPerRound; iter+=2)
		{
			for (int clientIndex=0; clientIndex < fanout; ++clientIndex)
			{
				short toPartnerId = 0;
				boolean yetPresent = true;

				while (yetPresent)
				{
					toPartnerId = (short) generator.nextInt(nodesNbr);
					yetPresent = (toPartnerId == selfId);
					for (int prevClient=0; prevClient < clientIndex; ++prevClient)
					{
						if (clientsId[prevClient] == toPartnerId)
						{
							yetPresent = true;
							break;
						}
					}
				}
				clientsId[clientIndex] = toPartnerId;
				injectUpdates(toPartnerId, iter, 2);
			}
		}

		++nbrSentUpdates; // il y a peut-être un décalage

		++this.roundId;
	}

	public void injectUpdates(short toPartnerId, int updateBase, int nbrUpdates)
	{
		ArrayList<Integer> updates = new ArrayList<Integer>();
		for (int i = updateBase; i < updateBase + nbrUpdates; ++i)
			updates.add(updatesNbrPerRound*(roundId-1) + 1 + i);
		Message sendMessage = new Message(roundId, MessageType.BRIEF, selfId, toPartnerId, updates, null, 0);
		outMessages.add(sendMessage);
	}

	public int getNbrSentUpdates() 
	{
		return nbrSentUpdates;
	}

}
