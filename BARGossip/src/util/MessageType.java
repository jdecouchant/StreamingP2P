package util;

public class MessageType 
{	
	public static final char EMPTY_UPDATES = 0;
	public static final char BAL = 1;
	public static final char BAL_RESP = 2;
	public static final char PUSH = 3;
	public static final char PUSH_RESP = 4;
	public static final char DIVULGE = 5;
	public static final char BRIEF = 6;
	public static final char KEY_RQST = 7;
	public static final char KEY_RESP = 8;
}

// log_svrEnd, log_svrBegin, peer_end : do not exist in the real protocol