package util;

import java.util.ArrayList;


public class BufferMap {		
	private ArrayList<Integer> orderedUpdatesId;

	// Parallel arraylists
	private ArrayList<Integer> updatesId; // The list of updates that the node received
	private ArrayList<Integer> senderId; // List of nodes that sent each update (used not to propose them their update)
	private ArrayList<Integer> roundAtWhichUpdateWasReceived;

	private int RTE;
	private int NBR_UPDATES_PER_ROUND;

	public BufferMap(int RTE, int NBR_UPDATES_PER_ROUND) {
		updatesId = new ArrayList<Integer>();
		senderId = new ArrayList<Integer>();
		roundAtWhichUpdateWasReceived = new ArrayList<Integer>();
		orderedUpdatesId = new ArrayList<Integer> ();
		this.RTE = RTE;
		this.NBR_UPDATES_PER_ROUND = NBR_UPDATES_PER_ROUND;
	}

	public int getBMSize()	{
		return updatesId.size();
	}

	// Return all received updates (for new partners)
	public ArrayList<Integer> getUpdates(int roundId) {
		return updatesId;
	}

	// Return the updates received during previous round (for established partners)
	public ArrayList<Integer> getLastRoundUpdates(int roundId, int toNodeId) {
		ArrayList<Integer> updatesNewlyReceived = new ArrayList<Integer>();
		for (int i = 0; i < roundAtWhichUpdateWasReceived.size(); ++i) {
			if (roundAtWhichUpdateWasReceived.get(i) == roundId - 1 && senderId.get(i) != toNodeId)
				updatesNewlyReceived.add(updatesId.get(i));
		}
		return updatesNewlyReceived;
	}

	public int getUpdatesId(int updatePos) {
		return updatesId.get(updatePos);
	}
	
	public int getSenderId(int updatePos) {
		return senderId.get(updatePos);
	}

	public boolean isPerempted(int roundId, int updateId) {
		return (updateId <= (roundId - RTE) * NBR_UPDATES_PER_ROUND);
	}

	public boolean isInserted(int updateId) {
		return updatesId.contains(updateId);
	}

	public void insertUpdate(int roundId, int updateId, int fromNodeId) {
		for (int posUpd = orderedUpdatesId.size() - 1; posUpd >= 0; --posUpd) {
			if (orderedUpdatesId.get(posUpd) == updateId) {
				orderedUpdatesId.remove(posUpd);
				break;
			}
		}

		if (isPerempted(roundId, updateId)) // Only defensive: should not happen!
			System.out.println("\n\n\n (BufferMap) WARNING : Perempted update\n\n");
		else if (!updatesId.contains(updateId)) {
			updatesId.add(updateId);
			roundAtWhichUpdateWasReceived.add(roundId);
			senderId.add(fromNodeId);
		}
	}

	public int actualize(int roundId) {
		int nbrDeletedUpdates = 0;
		for (int bmIndex = updatesId.size()-1; bmIndex >= 0; --bmIndex) {
			if (isPerempted(roundId, updatesId.get(bmIndex))) {
				++nbrDeletedUpdates;
				updatesId.remove(bmIndex);
				roundAtWhichUpdateWasReceived.remove(bmIndex);
				senderId.remove(bmIndex);
			}
		}
		return nbrDeletedUpdates;
	}

	public boolean wasOrdered(int updateId) {
		return orderedUpdatesId.contains(updateId);
	}

	public void insertOrderedUpdate(int updateId) {
		orderedUpdatesId.add(updateId);
	}

}
