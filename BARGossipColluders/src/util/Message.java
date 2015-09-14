package util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

public class Message implements Externalizable {
	
	public byte messageType;
	public int roundId;
	public short senderId;
	public short receiverId;
	public int[] updatesId;
	public int[] oldList;

	public double additionalSize; // TODO: to use for authenticators, and log entries

	public Message(int roundId, byte messageType, short senderId, short receiverId, ArrayList<Integer> updatesId, ArrayList<Integer> oldList, double additionalSize) 
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
	
	// Required by externalizable
	public Message() {}

	public int getRoundId()
	{
		return this.roundId;
	}

	public byte getMessageType() 
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

	
	public void writeExternal(ObjectOutput out) throws IOException {
		// TODO Auto-generated method stub
		out.writeByte(messageType);
		out.writeInt(roundId);
		out.writeShort(senderId);
		out.writeShort(receiverId);
		
		byte updatesLen = (updatesId == null)? 0: (byte) updatesId.length;
		out.writeByte(updatesLen);
		for (int i = 0; i < updatesLen; ++i)
			out.writeInt(updatesId[i]);
		
		byte oldListLen = (oldList == null)? 0: (byte) oldList.length;
		out.writeByte(oldListLen);
		for (int i = 0; i < oldListLen; ++i)
			out.writeInt(oldList[i]);
	}
	
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		messageType = in.readByte();
		roundId = in.readInt();
		senderId = in.readShort();
		receiverId = in.readShort();
		
		int updatesLen = in.readByte();
		updatesId = new int[updatesLen];
		for (int i = 0; i < updatesLen; ++i)
			updatesId[i] = in.readInt();
		
		int oldListLen = in.readByte();
		oldList = new int[oldListLen];
		for (int i = 0; i < oldListLen; ++i)
			oldList[i] = in.readInt();
	}
}
