package util;

import java.util.ArrayList;

public class BufferMap 
{

	private ArrayList<Integer> orderedUpdatesId;
	private ArrayList<Integer> updatesId;

	private int rte;
	private int updNbrPerRound;

	public BufferMap(int rte, int updNbrPerRound) 
	{
		updatesId = new ArrayList<Integer>();
		orderedUpdatesId = new ArrayList<Integer> ();
		this.rte = rte;
		this.updNbrPerRound = updNbrPerRound;
	}

	public int getBMSize()
	{
		return updatesId.size();
	}
	
	// Return all received updates (for new partners)
	public ArrayList<Integer> getUpdates(int roundId)
	{
		return updatesId;
	}

	public int getUpdatesId(int updatePos)
	{
		return updatesId.get(updatePos);
	}

	public boolean isPerempted(int roundId, int updateId)
	{
		return (updateId <= (roundId - rte) * updNbrPerRound);
	}

	public boolean isInserted(int updateId)
	{
		return updatesId.contains(updateId);
	}

	public void insertUpdate(int roundId, int updateId)
	{
		for (int posUpd = orderedUpdatesId.size() - 1; posUpd >= 0; --posUpd)
		{
			if (orderedUpdatesId.get(posUpd) == updateId)
				orderedUpdatesId.remove(posUpd);
		}
		
		if (!isPerempted(roundId, updateId) && !updatesId.contains(updateId))
		{
			updatesId.add(updateId);
		}
	}
	
	public long actualize(int roundId)
	{
		long nbrDeletedUpdates = 0;
		int bmIndex = updatesId.size() - 1;
		while (bmIndex >= 0)
		{
			if (isPerempted(roundId, updatesId.get(bmIndex)))
			{
				++nbrDeletedUpdates;
				updatesId.remove(bmIndex);
			}
			--bmIndex;
		}
		return nbrDeletedUpdates;
	}

	public boolean wasOrdered(int updateId)
	{
		return orderedUpdatesId.contains(updateId);
	}

	public void insertOrderedUpdate(int updateId)
	{
		orderedUpdatesId.add(updateId);
	}

}
