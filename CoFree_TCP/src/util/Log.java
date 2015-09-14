package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;

public class Log 
{
    // Constants for the size of the messages, in bits
    public static final int LOG_ENTRY_ID_SIZE = 20; // 1000000 messages per session
    public static final int ROUND_ID_SIZE = 13; // 2.27 h
    public static final int PARTNER_ID_SIZE = 12; // 4096 nodes at most
    public static final int MSG_TYPE_SIZE = 3;
    public static final int UPDATES_ID_SIZE = 18; // 1.82 h
    public static final int HASH_SIZE = 160; // SHA-1
    public static final int SIGN_SIZE = 1024; // RSA 1024
    public static final int AUTHENTICATOR_SIZE = LOG_ENTRY_ID_SIZE + HASH_SIZE + SIGN_SIZE;

    // Attributes
    private int selfId;
    private int nbrRoundsInLog;

    private boolean lastRoundLogSizeIsAvailable;
    private int lastRoundLogSize;

    private int logSize;

    private ArrayList<LogEntry> logEntries;
    private ArrayList<Integer> authenticators;

    // Constructor
    public Log(int nbrRoundsInLog, int selfId) 
    {
        this.selfId = selfId;
        this.nbrRoundsInLog = nbrRoundsInLog;

        this.lastRoundLogSize = 0;
        this.lastRoundLogSizeIsAvailable = false;

        this.logSize = 0;

        this.logEntries = new ArrayList<LogEntry>();
        this.authenticators = new ArrayList<Integer>();
    }

    public static void SHAsum(int sizeToConvert) throws NoSuchAlgorithmException
    {
        byte[] convertme = new byte[sizeToConvert];
        new Random().nextBytes(convertme);
        MessageDigest md = MessageDigest.getInstance("SHA-1"); 
        md.digest(convertme);
    }

    public int getLastRoundLogSize(int roundId)
    {
        if (lastRoundLogSizeIsAvailable)
            return lastRoundLogSize;

        lastRoundLogSize = 0;
        for (int i = 0; i < logEntries.size(); ++i)
        {
            if (logEntries.get(i).getRoundId() == roundId - 1)
                lastRoundLogSize += logEntries.get(i).getLogEntrySize();
        }
        lastRoundLogSizeIsAvailable = true;
        return (int) Math.ceil((double) lastRoundLogSize / (double) 8);
    }

    public int getLogSize() {       
        return (int) Math.ceil((double) logSize / (double) 8);
    }

    public int getAuthenticatorsSize()
    {
        int size = 0;
        for (int i = 0; i < authenticators.size(); ++i)
            size += authenticators.get(i);
        size *= AUTHENTICATOR_SIZE;
        return size;
    }

    public int getTotalSize() {
        return getLogSize() + getAuthenticatorsSize();
    }

    public void newRound(int roundId) {
        // TODO: remove old entries of log
        for (int i = logEntries.size() - 1; i >= 0; --i)
        {
            if (roundId - logEntries.get(i).getRoundId() >= nbrRoundsInLog)
            {
                logSize -= logEntries.get(i).getLogEntrySize();
                logEntries.remove(i);
                authenticators.remove(i);
            }
        }
        lastRoundLogSizeIsAvailable = false;
    }

    public void logMessage(Message message)
    {
        short partnerId = (message.senderId == selfId)? message.receiverId: message.senderId;
        LogEntry newLogEntry;
        if (message.messageType != MessageType.SERVE)
            newLogEntry = new LogEntry(message.roundId, partnerId, message.messageType, 
                    message.getUpdatesIdSize(), message.getUpdatesId());
        else // The content of the serve message is not logged, a hash replace it
            newLogEntry = new LogEntry(message.roundId, partnerId, message.messageType, 
                    message.getUpdatesIdSize(), null);

        logSize += newLogEntry.getLogEntrySize();
        logEntries.add(newLogEntry);

        if (message.senderId == selfId && message.messageType == MessageType.PROPOSE)
            authenticators.add(1);
        else
            authenticators.add(2);
    }

    public ArrayList<Short> getPreviousPartnersId(int selfId)
    {
        ArrayList<Short> partners = new ArrayList<Short>();
        for (int i = 0; i < logEntries.size(); ++i)
        {
            LogEntry current = logEntries.get(i);
            if (! partners.contains(current.getPartnerId()))
                partners.add(current.getPartnerId());
        }
        return partners;
    }

}
