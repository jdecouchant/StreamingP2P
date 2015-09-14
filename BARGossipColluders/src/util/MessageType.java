package util;

public class MessageType 
{	
	public static final byte EMPTY_UPDATES = 0;
	public static final byte BAL = 1;
	public static final byte BAL_RESP = 2;
	public static final byte PUSH = 3;
	public static final byte PUSH_RESP = 4;
	public static final byte DIVULGE = 5;
	public static final byte BRIEF = 6;
	public static final byte KEY_RQST = 7; 
	public static final byte KEY_RESP = 8;
	public static final byte COLLUDERS = 9; // Colluders use this message to propagate updates between themselves
}

// log_svrEnd, log_svrBegin, peer_end : do not exist in the real protocol