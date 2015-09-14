package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class BufferMap {        
    
    private class UpdateInfo {
        private int senderId;
        private int receptionRound;
        
        public UpdateInfo(int senderId, int receptionRound) {
            this.senderId = senderId;
            this.receptionRound = receptionRound;
        }
    }
    
    private HashSet<Integer> orderedUpdates;
    private HashMap<Integer, UpdateInfo> updates;

    private int RTE;
    private int NBR_UPDATES_PER_ROUND;
    private final boolean DEBUG;

    public BufferMap(int RTE, int NBR_UPDATES_PER_ROUND, boolean DEBUG) {
        this.RTE = RTE;
        this.NBR_UPDATES_PER_ROUND = NBR_UPDATES_PER_ROUND;
        updates = new HashMap<Integer, BufferMap.UpdateInfo>(RTE*NBR_UPDATES_PER_ROUND);
        orderedUpdates = new HashSet<Integer>();
        this.DEBUG = DEBUG;
    }

    // Return all received updates (for new partners)
    public Set<Integer> getUpdates() {
        return updates.keySet();
    }

    // Return the updates received during previous round (for established partners)
    public ArrayList<Integer> getLastRoundUpdates(int roundId, int toNodeId) {
        ArrayList<Integer> updatesNewlyReceived = new ArrayList<Integer>();
        Iterator<Integer> updatesIdItr= updates.keySet().iterator();
        while (updatesIdItr.hasNext()) {
            int updateId = updatesIdItr.next();
            UpdateInfo updateInfo = updates.get(updateId);
            if (updateInfo.receptionRound == roundId - 1 && updateInfo.senderId != toNodeId)
                updatesNewlyReceived.add(updateId);
        }
        return updatesNewlyReceived;
    }

    public int getSenderId(int updateId) {
        return updates.get(updateId).senderId;
    }

    public boolean isPerempted(int roundId, int updateId) {
        return (updateId <= (roundId - RTE) * NBR_UPDATES_PER_ROUND);
    }

    public boolean isInserted(int updateId) {
        return updates.containsKey(updateId);
    }

    public void insertUpdate(int roundId, int updateId, int fromNodeId) {
        if (DEBUG && !orderedUpdates.remove(updateId))
            System.out.println("WARNING (BufferMap): Received non ordered update!");

        if (DEBUG && isPerempted(roundId, updateId)) // Only defensive: should not happen!
            System.out.println("WARNING (BufferMap): Received perempted update!");
        else if (!updates.containsKey(updateId))
            updates.put(updateId, new UpdateInfo(fromNodeId, roundId));
    }

    public int actualize(int roundId) {
        int nbrDeletedUpdates = 0;
        for (Iterator<Map.Entry<Integer, UpdateInfo>> it = updates.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, UpdateInfo> entry = it.next();
            if (isPerempted(roundId, entry.getKey())) {
                ++nbrDeletedUpdates;
                it.remove();
            }
        }
        return nbrDeletedUpdates;
    }

    public boolean isOrdered(int updateId) {
        return orderedUpdates.contains(updateId);
    }

    public void addOrderedUpdate(int updateId) {
        orderedUpdates.add(updateId);
    }

}
