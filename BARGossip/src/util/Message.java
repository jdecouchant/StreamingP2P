package util;

import java.io.Serializable;
import java.util.ArrayList;

public class Message implements Serializable {

	private static final long serialVersionUID = 1L;
	//	static final long serialVersionUID = 42L;	

	public int roundId;
	public char messageType;
	public short senderId;
	public short receiverId;
	public int[] updatesId;
	public int[] oldList;

	public double additionalSize; // TODO: to use for authenticators, and log entries

	public Message(int roundId, char messageType, short senderId, short receiverId, ArrayList<Integer> updatesId, ArrayList<Integer> oldList, double additionalSize) 
	{
		this.roundId = roundId;
		this.messageType = messageType;
		this.senderId = senderId;
		this.receiverId = receiverId;
		
		if (updatesId != null)
		{
			this.updatesId = new int[updatesId.size()];
			for (int i=0; i<updatesId.size(); ++i)
				this.updatesId[i] = updatesId.get(i);
		}
		
		if (oldList != null)
		{
			this.oldList = new int[oldList.size()];
			for (int i=0; i<oldList.size(); ++i)
				this.oldList[i] = oldList.get(i);
		}

		this.additionalSize = additionalSize; 
	}

	public int getRoundId()
	{
		return this.roundId;
	}

	public char getMessageType() 
	{
		return messageType;
	}

	public short getSenderId() 
	{
		return senderId;
	}
	
	public short getReceiverId() 
	{
		return receiverId;
	}

	public double getAdditionalSize() 
	{
		return additionalSize;
	}
	
	public int getUpdatesIdSize()
	{
		if (updatesId == null)
			return 0;
		return updatesId.length;
	}
	
	public int getUpdatesId(int i)
	{
		return updatesId[i];
	}
	
	public int getOldListSize()
	{
		if (oldList == null)
			return 0;
		return oldList.length;
	}
	
	public int getOldListId(int i)
	{
		return oldList[i];
	}
}
