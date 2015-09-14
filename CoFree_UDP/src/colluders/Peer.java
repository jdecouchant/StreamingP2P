package colluders;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import util.BufferMap;
import util.Log;
import util.Message;
import util.MessageType;
import util.Node;

// TODO: if a node tries to interact with an absent node, interact with server to denounce it

public class Peer 
{
	// Attributes of peer
	public short selfId;
	private int roundId;

	// Membership
	private ArrayList<Node> nodesList;
	private Scenario scenario;
	private Set<Short> epochList, epochListPLus1;
	private Set<Short> epochAbsentList;
	private boolean needEpochListPLus1;

	private int tubSize; // Size of a tube at join time
	private ArrayList<Short> joinNodesList;

	// Owned updates and logs
	private BufferMap selfBM; // Contains the ids of the updates the node received
	private Log selfLog; 

	// Protocol parameters
	private int EPOCH; // Number of rounds for which an epoch list is valid
	private int FANOUT; // Number of partnerships a node has to maintain
	private int PERIOD; // Number of rounds for which a partnership is established
	private int MAX_UPD_EXCHANGE; // The maximum number of updates a peer can trade during a single round with a partner
	private int RTE; // Number of rounds between the release of updates and their consumption by the media player
	//	private int NBR_UPDATES_PER_ROUND; // Number of updates the server releases per round

	private Random generator;

	// State of current node
	private boolean selfIsWaitingToJoin, selfIsPresent, selfHasBeenEvicted;

	// Partners of node
	private ArrayList<Partner> selfPartners;

	// Thread and communication queue
	public Sender sender;
	public Receiver receiver;
	public Thread senderTh, receiverTh;
	public LinkedBlockingQueue<Message> inMessages, outMessages;

	public BufferedWriter outLog;

	private static final boolean DEBUG = false;
	private static final boolean OUTPUT = true;
	private static final int PROBA_AUDIT = 10;
	private static final int PROBA_TUB = 10;

	// Constructor
	Peer(short selfId, int listeningPort, ArrayList<Node> nodesList, int FANOUT, int PERIOD, int RTE, int NBR_UPDATES_PER_ROUND,
			int updatesLen, int scenarioId, int EPOCH) {

		// State of the node
		this.selfIsWaitingToJoin = false;
		this.selfIsPresent = false;
		this.selfHasBeenEvicted = false;

		this.selfId = selfId;

		this.EPOCH = EPOCH;
		this.FANOUT = FANOUT;
		this.PERIOD = PERIOD;
		this.RTE = RTE;
		this.MAX_UPD_EXCHANGE = 2 * NBR_UPDATES_PER_ROUND;
		//		this.NBR_UPDATES_PER_ROUND = NBR_UPDATES_PER_ROUND;
		
		this.nodesList = nodesList;

		this.scenario = new Scenario(scenarioId, nodesList.size(), PERIOD);
		this.epochList = new HashSet<Short>();
		this.epochListPLus1 = new HashSet<Short>();
		this.epochAbsentList = new HashSet<Short>(); // TODO: this list has to be exchanged between peers
		this.needEpochListPLus1 = true;

		this.roundId = 1;

		this.selfBM = new BufferMap(RTE, NBR_UPDATES_PER_ROUND);

		this.selfLog = new Log(RTE, selfId);

		this.generator = new Random(System.currentTimeMillis());
		this.selfPartners = new ArrayList<Partner>();

		// Create receiver : receive messages, extract useful data and put it into inMessages
		this.inMessages = new LinkedBlockingQueue<Message>();
		this.receiver = new Receiver(listeningPort, inMessages, selfId, this);
		this.receiverTh = new Thread(receiver);
		this.receiverTh.start();

		// Create sender : from useful data in outMessages, make a packet and send it
		this.outMessages = new LinkedBlockingQueue<Message>();
		this.sender = new Sender(selfId, outMessages, updatesLen, nodesList, this);
		this.senderTh = new Thread(sender);
		this.senderTh.start();

		if (OUTPUT) {
			try {
				outLog = new BufferedWriter(new FileWriter(selfId + "_logSize.txt"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		this.joinNodesList = null;
	}

	public int getRoundId() {
		return this.roundId;
	}
	
	public int getNodeStatus() {
		if (selfIsWaitingToJoin) 
			return 0; 
		else if (selfIsPresent) // Present
			return 2;
		else if (selfHasBeenEvicted)// Evicted
			return 3;
		else // Absent
			return 1;
	}

	// Methods
	// Used to collect final statistics

	public void newRound() {
		System.out.println("\nPeer " + selfId + ": round " + roundId);

		if (DEBUG) {
			if (selfIsWaitingToJoin)
				System.out.println("\tNode is waiting to join");
			else if (selfIsPresent)
				System.out.println("\tNode is present");
			else 
				System.out.println("\tNode is absent");
		}

		// Membership: does the node has to leave/join the session? 
		if (scenario.nodeMustQuit(selfId, roundId)) {
			sendQUIT_SESSION();
		} else if (scenario.nodeMustJoin(selfId, roundId) || (joinNodesList == null && selfIsWaitingToJoin)) {
			sendJOIN_SESSION();
		}

		if (roundId % EPOCH == 0 || roundId == 1) {

			epochList.addAll(epochListPLus1);

			if (DEBUG) {
				System.out.println("\tNew EPOCH");
				System.out.println("Nodes that are now in epoch list:" + epochList.toString());
			}

			if (epochList.contains(selfId) && selfIsWaitingToJoin) {
				selfIsPresent = true;
				selfIsWaitingToJoin = false;
			}

			this.epochListPLus1.clear();
			this.needEpochListPLus1 = true;
			this.epochAbsentList.clear();
		}

		if (selfIsPresent || selfIsWaitingToJoin) {
			// Actualize the list of partners
			for (int partnerPos = selfPartners.size() - 1; partnerPos >= 0; --partnerPos) {
				if (selfPartners.get(partnerPos).partnershipHasEnded())
					selfPartners.remove(partnerPos);
				else 
					selfPartners.get(partnerPos).newRound();
			}

			if (selfIsWaitingToJoin && joinNodesList != null) { 
				if ((roundId + selfId) % PERIOD == 0 || selfPartners.size() == 0) {
					// TODO: Should be better to keep in memory which nodes were selected, and check that they are not selected twice !!
					for (int clientIndex = 0; clientIndex < Math.min(FANOUT, joinNodesList.size()); ++clientIndex) {
						// Choose a tub id
						short toPartnerId;
						if (tubSize != 0) {
							int nbTubs = joinNodesList.size() / tubSize + 1;
							int tubId = 0;
							for (int i = 0; i < nbTubs-1; ++i) {
								++tubId;
								if (generator.nextInt(100) < PROBA_TUB)
									break;
							}
							// Choose position in the selected tub
							int lowerIndex = joinNodesList.size() - tubId*tubSize;
							int higherIndex = Math.min(joinNodesList.size(), lowerIndex + tubSize);

							int offset = generator.nextInt(higherIndex-lowerIndex);
							int toPartnerIndex = lowerIndex + offset;
							toPartnerId = joinNodesList.get(toPartnerIndex);
						} else {
							int toPartnerIndex = generator.nextInt(joinNodesList.size());
							toPartnerId = joinNodesList.get(toPartnerIndex);
						}
						sendASSOCIATION(toPartnerId);
					}
				} else {
					for (int partnerPos = 0; partnerPos < selfPartners.size(); ++partnerPos)
						sendASSOCIATION_MAINTAIN(selfPartners.get(partnerPos).getPartnerId());
				}
			} else if (selfIsPresent) {
				// Find new partners, and initiate associations with them
				if ((roundId + selfId) % PERIOD == 0 || selfPartners.size() == 0) {
					ArrayList<Short> availableNodesId = new ArrayList<Short>();
					Iterator<Short> it = epochList.iterator();
					if (epochList.size() == 0) // Handle the join case
						it = epochListPLus1.iterator();
					while (it.hasNext()) {
						short nodeId = it.next();
						if (nodeId != selfId && nodeId > 0) // TODO: nodeId>0 should not be necessary
							availableNodesId.add(nodeId);
					}
					for (int clientIndex = 0; clientIndex < Math.min(FANOUT, availableNodesId.size()); ++clientIndex) {
						int toPartnerIndex = generator.nextInt(availableNodesId.size());
						short toPartnerId = availableNodesId.get(toPartnerIndex);

						availableNodesId.remove(toPartnerIndex);
						sendASSOCIATION(toPartnerId);
					}
				} else {
					for (int partnerPos = 0; partnerPos < selfPartners.size(); ++partnerPos)
						sendASSOCIATION_MAINTAIN(selfPartners.get(partnerPos).getPartnerId());
				}
			}
		}
	}

	// Need an extra procedure for the end of the round (treat in messages in the meanwhile)
	public int[] endOfRound() {
		int deletedUpdNbr = selfBM.actualize(roundId);
		System.out.println("Number of available updates that expired : " + deletedUpdNbr);

		if (OUTPUT) {
			try {
				outLog.write(selfLog.getLogSize() + "\t" + selfLog.getTotalSize() + "\n");
			} catch (IOException e) {}
		}
		selfLog.newRound(roundId);
		++this.roundId;	

		return new int[] {this.getNodeStatus(), deletedUpdNbr} ;
	}

	public void treatInMessage() {
		try {
			Message message = inMessages.poll(10, TimeUnit.MILLISECONDS);
			if (message != null) // Nodes can treat in messages if it joined the session
			{	
				if (selfIsPresent || selfIsWaitingToJoin) { 
					switch (message.messageType) {
					case MessageType.ASSOC:
						receiveASSOCIATION(message);
						break;
					case MessageType.ASSOC_RESP:
						receiveASSOCIATION_RESP(message);
						break;
					case MessageType.ASSOC_MAINTAIN:
						receiveASSOCIATION_MAINTAIN(message);
						break;
					case MessageType.ASSOC_MAINTAIN_RESP:
						receiveASSOCIATION_MAINTAIN_RESP(message);
						break;
					case MessageType.PROPOSE:
						selfLog.logMessage(message);
						receivePROPOSE(message);
						break;
					case MessageType.REQUEST:
						selfLog.logMessage(message);
						receiveREQUEST(message);
						break;
					case MessageType.SERVE:
						if (message.senderId != 0) // Serve messages are logged only when they come from a peer
							selfLog.logMessage(message);
						receiveSERVE(message);
						break;
					case MessageType.AUDIT:
						selfLog.logMessage(message);
						receiveAUDIT(message);
						break;
					case MessageType.AUDIT_RESP:
						receiveAUDIT_RESP(message);
						break;
					case MessageType.NODE_ABSENT:
						receiveNODE_ABSENT(message);
						break;
					case MessageType.EVICTION_NOTIFICATION:
						receiveEVICTION_NOTIFICATION(message);
						break;
					case MessageType.JOIN_SESSION_RESP:
						receiveJOIN_SESSION_RESP(message);
						break;
					}
				} else 
					sendNODE_ABSENT(message.senderId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**********************************************************/
	/*** MEMBERSHIP_MANAGEMENT                              ***/
	/**********************************************************/
	public void sendJOIN_SESSION()	{
		selfIsWaitingToJoin = true;
		selfIsPresent = false;
		Message sendMessage = new Message(roundId, MessageType.JOIN_SESSION, selfId, (short) 0);
		outMessages.add(sendMessage);
	}

	public void receiveJOIN_SESSION_RESP(Message message) {
		this.tubSize = message.additionalSize;
		this.joinNodesList = new ArrayList<Short>();
		for (int i = 0; i < message.getNodes1Size(); ++i)
			this.joinNodesList.add(message.nodes1[i]);
		epochList.addAll(joinNodesList);
	}

	public void sendQUIT_SESSION() {
		if (DEBUG)
			System.out.println("Node quit session");
		selfIsPresent = false;
		selfIsWaitingToJoin = false;
		epochList.clear();
		epochListPLus1.clear();
		Message sendMessage = new Message(roundId, MessageType.QUIT_SESSION, selfId, (short) 0);
		outMessages.add(sendMessage);
	}

	public void sendNODE_ABSENT(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.NODE_ABSENT, selfId, toPartnerId);
		outMessages.add(sendMessage);
	}

	public void receiveNODE_ABSENT(Message message) {
		if (DEBUG)
			System.out.println("Receive notifications that node " + message.senderId + " has left");

		short absentNodeId = message.senderId;
		epochList.remove(absentNodeId);
		epochListPLus1.remove(absentNodeId);
		epochAbsentList.add(absentNodeId);

		handlePartnerDeparture(absentNodeId);

		// We simulate that the node sends a message to the server to inform it that the node is absent
		// The server then should check that the node left, and reply with a token that the node has to include in its log
		// TODO: to check (is it necessary to send a message?)
		Message responseFromServer = new Message(roundId, MessageType.SERVER_TOKEN, (short) 0, selfId);
		selfLog.logMessage(responseFromServer);
	}

	public void receiveEVICTION_NOTIFICATION(Message message) {
		// The node has been evicted !!
		if (DEBUG)
			System.out.print("\n\n\nNODE HAS BEEN EVICTED BY SERVER !!!\n\n\n");
		selfIsPresent = false;
		selfIsWaitingToJoin = false;
		selfHasBeenEvicted = true;
		epochList.remove(selfId);
		epochListPLus1.remove(selfId);
	}

	/**********************************************************/
	/*** ASSOCIATION_RQST                                   ***/
	/**********************************************************/

	public void sendASSOCIATION(short toPartnerId) {
		Partner toPartner = new Partner(nodesList.get(toPartnerId), PERIOD, RTE, true);
		selfPartners.add(toPartner);

		Message sendMessage = new Message(roundId, MessageType.ASSOC, selfId, toPartnerId);
		sendMessage.setNodes2(epochAbsentList.toArray(new Short[epochAbsentList.size()]));
		sendMessage.setAdditionalSize(selfLog.getLastRoundLogSize(roundId));

		// Ask for partners list with given probability (for audits)
		if (generator.nextInt(100)*FANOUT < PROBA_AUDIT)
			sendMessage.setNeedPartnersOrEpochListPlus1(true);

		outMessages.add(sendMessage);
	}

	public void receiveASSOCIATION(Message message) {
		short toPartnerId = message.senderId;

		Partner toPartner = new Partner(nodesList.get(toPartnerId), PERIOD, RTE, false);
		selfPartners.add(toPartner);

		Set<Short> absentNodesPartnerDontKnow = new HashSet<Short>(epochAbsentList);
		for (int i = 0; i < message.getNodes2Size(); ++i) {
			short absentNodeId = message.nodes2[i];
			epochAbsentList.add(absentNodeId);
			epochList.remove(absentNodeId);
			epochListPLus1.remove(absentNodeId);
			absentNodesPartnerDontKnow.remove(absentNodeId);
		}

		sendASSOCIATION_RESP(toPartnerId, absentNodesPartnerDontKnow.toArray(new Short[absentNodesPartnerDontKnow.size()]));

		sendPROPOSE(toPartnerId, (message.needPartnersOrEpochListPlus1==1));

	}

	/**********************************************************/
	/*** ASSOCIATION_RESP                                   ***/
	/**********************************************************/
	public void sendASSOCIATION_RESP(short toPartnerId, Short[] absentNodes) {
		Message sendMessage = new Message(roundId, MessageType.ASSOC_RESP, selfId, toPartnerId);
		sendMessage.setAdditionalSize(selfLog.getLastRoundLogSize(roundId));
		sendMessage.setNodes1(selfLog.getPreviousPartnersId(selfId));
		sendMessage.setNodes2(absentNodes);

		if (generator.nextInt(100)*FANOUT < PROBA_AUDIT)
			sendMessage.setNeedPartnersOrEpochListPlus1(true);

		outMessages.add(sendMessage);
	}

	public void receiveASSOCIATION_RESP(Message message) {
		short toPartnerId = message.senderId;

		sendPROPOSE(toPartnerId, (message.needPartnersOrEpochListPlus1==1));

		for (int i = 0; i < message.getNodes2Size(); ++i) {
			short absentNodeId = message.nodes2[i];
			epochAbsentList.add(absentNodeId);
			epochList.remove(absentNodeId);
			epochListPLus1.remove(absentNodeId);
		}
	}

	/**********************************************************/
	/*** ASSOCIATION_MAINTAIN                               ***/
	/**********************************************************/

	public void sendASSOCIATION_MAINTAIN(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.ASSOC_MAINTAIN, selfId, toPartnerId); // Pas besoin des partenaires: audit seulement premières fois
		sendMessage.setAdditionalSize(selfLog.getLastRoundLogSize(roundId));
		outMessages.add(sendMessage);
	}

	public void receiveASSOCIATION_MAINTAIN(Message message) {
		short toPartnerId = message.senderId;

		sendASSOCIATION_MAINTAIN_RESP(toPartnerId);

		sendPROPOSE(toPartnerId, false);
	}

	/**********************************************************/
	/*** ASSOCIATION_MAINTAIN_RESP                          ***/
	/**********************************************************/

	public void sendASSOCIATION_MAINTAIN_RESP(short toPartnerId) {
		Message sendMessage = new Message(roundId, MessageType.ASSOC_MAINTAIN_RESP, selfId, toPartnerId);
		sendMessage.setAdditionalSize(selfLog.getLastRoundLogSize(roundId));
		outMessages.add(sendMessage);
	}

	public void receiveASSOCIATION_MAINTAIN_RESP(Message message) {
		sendPROPOSE(message.senderId, false);
	}

	/**********************************************************/
	/*** PROPOSE                                            ***/
	/**********************************************************/
	
	public void sendPROPOSE(short toPartnerId, boolean needPartnersId) {
		Message sendMessage;

		for (int i = 0; i < selfPartners.size(); ++i) {
			if (selfPartners.get(i).getPartnerId() == toPartnerId) {
				ArrayList<Integer> updates;
				if (selfPartners.get(i).isNewPartner()) {
					sendMessage = new Message(roundId, MessageType.PROPOSE, selfId, toPartnerId);
					updates = new ArrayList<Integer>(selfBM.getUpdates(roundId));
				} else {
					sendMessage = new Message(roundId, MessageType.PROPOSE, selfId, toPartnerId);
					updates = new ArrayList<Integer>(selfBM.getLastRoundUpdates(roundId, toPartnerId));
				}
				// If node and the sender of the updates are colluders, do not propose the update
				if (scenario.nodeIsColluder(roundId, selfId)) {
					for (int updatePos = updates.size()-1; updatePos >= 0; --updatePos) {
						if (scenario.nodeIsColluder(roundId, selfBM.getSenderId(updatePos)))
							updates.remove(updatePos);
					}
				}
				sendMessage.setUpdatesId(updates);
				if (needPartnersId)
					sendMessage.setNodes1(selfLog.getPreviousPartnersId(selfId));

				selfLog.logMessage(sendMessage);			
				outMessages.add(sendMessage);
				break;
			}
		}


	}

	public void receivePROPOSE(Message message) {
		ArrayList<Integer> canReceive = new ArrayList<Integer>();
		for (int i = 0; i < message.getUpdatesIdSize(); ++i) {
			int update = message.updatesId[i];
			if (!selfBM.wasOrdered(update) && !selfBM.isInserted(update) && 
					!selfBM.isPerempted(roundId, update) && (int) canReceive.size() < MAX_UPD_EXCHANGE) {
				selfBM.insertOrderedUpdate(update);
				canReceive.add(update);
			}
		}
		if (canReceive.size() != 0)
			sendREQUEST(message.senderId, canReceive);

		if (message.getNodes1Size() != 0) {
			ArrayList<Short> nodesToAudit = new ArrayList<Short>();
			for (int i = 0; i < message.getNodes1Size(); ++i) {
				if (!nodesToAudit.contains(message.nodes1[i]))
					nodesToAudit.add(message.nodes1[i]);
			}

			for (int i = 0; i < nodesToAudit.size(); ++i)
				sendAUDIT(nodesToAudit.get(i));
		}
	}

	/**********************************************************/
	/*** AUDIT                                              ***/
	/**********************************************************/

	public void sendAUDIT(short toPartnerId) {
		if (DEBUG)
			System.out.println("Node " + selfId + ": Send audit to "+toPartnerId);

		Message sendMessage = new Message(roundId, MessageType.AUDIT, selfId, toPartnerId);
		selfLog.logMessage(sendMessage);
		outMessages.add(sendMessage);
	}

	public void receiveAUDIT(Message message) {
		short toPartnerId = message.senderId;
		Message sendMessage = new Message(roundId, MessageType.AUDIT_RESP, selfId, toPartnerId);

		selfLog.logMessage(sendMessage);
		sendMessage.setAdditionalSize(selfLog.getLogSize());
		outMessages.add(sendMessage);
	}

	public void receiveAUDIT_RESP(Message message) {
		short toPartnerId = message.senderId;
		if (scenario.nodeIsColluder(roundId, toPartnerId)) {
			denounceIncorrectNode(toPartnerId, selfLog.getLogSize() * 2); // We consider that two logs are necessary to prove that a node is a colluder
		}
	}

	/**********************************************************/
	/*** REQUEST                                            ***/

	public void sendREQUEST(short toPartnerId, ArrayList<Integer> updList) {
		Message sendMessage = new Message(roundId, MessageType.REQUEST, selfId, toPartnerId);
		sendMessage.setNeedPartnersOrEpochListPlus1(this.needEpochListPLus1);
		sendMessage.setUpdatesId(updList);
		selfLog.logMessage(sendMessage);
		outMessages.add(sendMessage);
	}

	public void receiveREQUEST(Message message) {
		ArrayList<Integer> updList = new ArrayList<Integer>();
		for (int i = 0; i < message.getUpdatesIdSize(); ++i)
			updList.add(message.updatesId[i]);

		sendSERVE(message.senderId, updList, (message.needPartnersOrEpochListPlus1==1));
	}

	/**********************************************************/
	/*** SERVE                                              ***/
	/**********************************************************/

	public void sendSERVE(short toPartnerId, ArrayList<Integer> updList, boolean partnerNeedEpochListPlus2) {
		Short absentNodes[] = epochAbsentList.toArray(new Short[epochAbsentList.size()]);

		Message sendMessage = new Message(roundId, MessageType.SERVE, selfId, toPartnerId);

		sendMessage.setUpdatesId(updList);
		sendMessage.setNodes2(absentNodes);
		if (partnerNeedEpochListPlus2) {
			Short nodeOfNextEpoch[] = epochListPLus1.toArray(new Short[epochListPLus1.size()]);
			sendMessage.setNodes1(nodeOfNextEpoch);
		}

		selfLog.logMessage(sendMessage);

		outMessages.add(sendMessage);
	}

	public void receiveSERVE(Message message) {
		if (message.senderId != 0) {
			System.out.println("Receive serve at round " + roundId + " from node " + message.senderId);
			System.out.println(Arrays.toString(message.updatesId));
		}

		for (int i = 0; i < message.getUpdatesIdSize(); ++i)
			selfBM.insertUpdate(roundId, message.updatesId[i], message.senderId);

		// Receive membership list (leave notifications)
		for (int i = 0; i < message.getNodes2Size(); ++i) {
			short absentNodeId = message.nodes2[i];
			epochAbsentList.add(absentNodeId);
			epochList.remove(absentNodeId);
		}

		if (message.getNodes1Size() > 0) {
			this.needEpochListPLus1 = false;
			for (int i = 0; i < message.getNodes1Size(); ++i) {
				if (DEBUG)
					System.out.println("\tNode " + message.nodes1[i] + " is in epochListPlus1");
				this.epochListPLus1.add(message.nodes1[i]);
				if (selfId == message.nodes1[i]) {
					joinNodesList = null;
					selfIsPresent = true;
					selfIsWaitingToJoin = false;
				}
			}
		}
	}

	public void handlePartnerDeparture(short partnerId) {
		// A partner of the node was potentially absent or incorrect: remove it from partners, and find a new partner if necessary
		ArrayList<Short> availableNodes = new ArrayList<Short>(epochList);
		boolean hasToFindNewPartner = false; // a node has to find a new partner if a node it initiated interactions with left
		for (int partnerPos = selfPartners.size() - 1; partnerPos >= 0; --partnerPos) {
			if (selfPartners.get(partnerPos).getPartnerId() == partnerId) {
				hasToFindNewPartner = selfPartners.get(partnerPos).getInitiatedByNode();
				selfPartners.remove(partnerPos);
				break;
			} 
		}

		// Find a new partner to replace the one that left (possible nodes are in availableNodes)
		if (hasToFindNewPartner && availableNodes.size() > 0) {
			int positionNewNode = generator.nextInt(availableNodes.size());
			sendASSOCIATION(availableNodes.get(positionNewNode));
		}
	}

	public void denounceIncorrectNode(short incorrectNodeId, int logSizes) {
		if (DEBUG)
			System.out.println("Denounce node " + incorrectNodeId);
		epochAbsentList.add(incorrectNodeId);
		epochList.remove(incorrectNodeId);
		epochListPLus1.remove(incorrectNodeId);

		handlePartnerDeparture(incorrectNodeId);

		Message sendMessage = new Message(roundId, MessageType.POM, incorrectNodeId, (short) 0);
		sendMessage.setAdditionalSize(logSizes);
		selfLog.logMessage(sendMessage);
		outMessages.add(sendMessage);
	}

}


