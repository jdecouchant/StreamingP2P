package util;

public class MessageType {
    public static final byte EMPTY_UPDATES = 0; // used only to simulate bandwidth consumption (size of updates)
    public static final byte EMPTY_LOG = 1;
    public static final byte ASSOC = 2;
    public static final byte ASSOC_RESP = 3;
    public static final byte ASSOC_MAINTAIN = 4;
    public static final byte ASSOC_MAINTAIN_RESP = 5;
    public static final byte PROPOSE = 6;
    public static final byte REQUEST = 7;
    public static final byte SERVE = 8;
    public static final byte AUDIT = 9;
    public static final byte AUDIT_RESP = 10;

    public static final byte JOIN_SESSION = 11;
    public static final byte JOIN_SESSION_RESP = 12;

    public static final byte QUIT_SESSION = 13;
    public static final byte NODE_ABSENT = 14;
    public static final byte POM = 15;
    public static final byte EVICTION_NOTIFICATION = 16;
    public static final byte SERVER_TOKEN = 17; // Never transmitted over the network, only stored in the log
}

// Remark: log_svrEnd, log_svrBegin, peer_end : do not exist in the real protocol