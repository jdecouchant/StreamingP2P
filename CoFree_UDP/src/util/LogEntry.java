package util;

public class LogEntry 
{	
	private int roundId;
	private short partnerId;

	private byte messageType;
	private int[] updatesId;

	public LogEntry(int roundId, short partnerId, byte messageType, 
			int updatesIdNbr, int[] updatesId)
	{
		this.roundId = roundId;
		this.partnerId = partnerId;
		this.messageType = messageType;

		if (updatesId != null)
		{
			this.updatesId = new int[updatesIdNbr];
			for (int i=0; i<updatesIdNbr; ++i)
				this.updatesId[i] = updatesId[i];
		} 
		try {Log.SHAsum(this.getLogEntrySize());} catch (Exception e) {e.printStackTrace();}
	}

	public int getRoundId()
	{
		return this.roundId;
	}

	public short getPartnerId()
	{
		return this.partnerId;
	}

	// Return the size of this log entry in bits
	public int getLogEntrySize()
	{
		int res = Log.LOG_ENTRY_ID_SIZE;

		res += Log.ROUND_ID_SIZE;
		res += Log.PARTNER_ID_SIZE;
		res += Log.MSG_TYPE_SIZE;

		if (updatesId != null)
			res += Log.UPDATES_ID_SIZE * updatesId.length;
		else if (messageType == MessageType.SERVE)
			res += Log.HASH_SIZE;
		
		res += Log.HASH_SIZE;
		
		return res;
	}


}
