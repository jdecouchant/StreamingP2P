package util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;

public class Message implements Externalizable {

    // Header
    public int roundId;
    public byte messageType;
    public short senderId;
    public short receiverId;

    // Additional attributes
    public short[] nodes1;
    public short[] nodes2;
    public int[] updatesId;

    public byte needPartnersOrEpochListPlus1;
    public int additionalSize; // Used for authenticators, and log entries

    public Message(int roundId, byte messageType, short senderId,
            short receiverId) {
        this.roundId = roundId;
        this.messageType = messageType;
        this.senderId = senderId;
        this.receiverId = receiverId;

        this.additionalSize = 0;
        this.needPartnersOrEpochListPlus1 = 0;
    }

    public Message() {
    }

    /**
     * Setters
     */

    public void setNodes1(ArrayList<Short> nodesId) {
        if (nodesId != null) {
            this.nodes1 = new short[nodesId.size()];
            for (int i = 0; i < nodesId.size(); ++i)
                this.nodes1[i] = nodesId.get(i);
        }
    }

    public void setNodes1(Short nodesId[]) {
        if (nodesId.length != 0) {
            this.nodes1 = new short[nodesId.length];
            for (int i = 0; i < nodesId.length; ++i)
                this.nodes1[i] = nodesId[i];
        }
    }

    public void setUpdatesId(ArrayList<Integer> updatesId) {
        if (updatesId != null) {
            this.updatesId = new int[updatesId.size()];
            for (int i = 0; i < updatesId.size(); ++i)
                this.updatesId[i] = updatesId.get(i);
        }
    }

    public void setNodes2(Short nodesId[]) {
        if (nodesId.length != 0) {
            this.nodes2 = new short[nodesId.length];
            for (int i = 0; i < nodesId.length; ++i)
                this.nodes2[i] = nodesId[i];
        }
    }

    public void setNeedPartnersOrEpochListPlus1(boolean needEpochListPlus1) {
        this.needPartnersOrEpochListPlus1 = (needEpochListPlus1) ? (byte) 1 : 0;
    }

    public void setAdditionalSize(int additionalSize) {
        this.additionalSize = additionalSize;
    }

    /**
     * Getters
     */

    public int getNodes1Size() {
        if (nodes1 == null)
            return 0;
        return nodes1.length;
    }

    public int getNodes2Size() {
        if (nodes2 == null)
            return 0;
        return nodes2.length;
    }

    public int getUpdatesIdSize() {
        if (updatesId == null)
            return 0;
        return updatesId.length;
    }

    public int[] getUpdatesId() {
        return updatesId;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(messageType);
        out.writeInt(roundId);
        out.writeShort(senderId);
        out.writeShort(receiverId);

        short nodes1Len = (nodes1 == null) ? 0 : (short) nodes1.length;
        out.writeShort(nodes1Len);
        for (int i = 0; i < nodes1Len; ++i)
            out.writeShort(nodes1[i]);

        short nodes2Len = (nodes2 == null) ? 0 : (short) nodes2.length;
        out.writeShort(nodes2Len);
        for (int i = 0; i < nodes2Len; ++i)
            out.writeShort(nodes2[i]);

        short updatesLen = (updatesId == null) ? 0 : (short) updatesId.length;
        out.writeShort(updatesLen);
        for (int i = 0; i < updatesLen; ++i)
            out.writeInt(updatesId[i]);

        if (nodes1Len < 0 || nodes2Len < 0 || updatesLen < 0) {
            System.out.println("OVERFLOW: (" + nodes1Len + ", " + nodes2Len
                    + ", " + updatesLen + ") -> ("
                    + ((nodes1 == null) ? 0 : nodes1.length) + ", "
                    + ((nodes2 == null) ? 0 : nodes2.length) + ", "
                    + ((updatesId == null) ? 0 : updatesId.length) + ")");
        }
    }

    public void readExternal(ObjectInput in) throws IOException,
    ClassNotFoundException {
        messageType = in.readByte();
        roundId = in.readInt();
        senderId = in.readShort();
        receiverId = in.readShort();

        short nodes1Len = in.readShort();
        nodes1 = new short[nodes1Len];
        for (int i = 0; i < nodes1Len; ++i)
            nodes1[i] = in.readShort();

        short nodes2Len = in.readShort();
        nodes2 = new short[nodes2Len];
        for (int i = 0; i < nodes2Len; ++i)
            nodes2[i] = in.readShort();

        short updatesLen = in.readShort();
        updatesId = new int[updatesLen];
        for (int i = 0; i < updatesLen; ++i)
            updatesId[i] = in.readInt();
    }

    public String toString() {
        String s = "<";
        s += messageType + ", " + roundId + ", " + senderId + ", " + receiverId;

        byte nodes1Len = (nodes1 == null) ? 0 : (byte) nodes1.length;
        s += ", " + nodes1Len + "[";
        for (int i = 0; i < nodes1Len; ++i)
            s += nodes1[i] + ", ";
        s += "], ";

        byte nodes2Len = (nodes2 == null) ? 0 : (byte) nodes2.length;
        s += nodes2Len + "[";
        for (int i = 0; i < nodes2Len; ++i)
            s += nodes2[i] + ", ";
        s += "], ";

        byte updatesLen = (updatesId == null) ? 0 : (byte) updatesId.length;
        s += updatesLen + "[";
        for (int i = 0; i < updatesLen; ++i)
            s += updatesId[i] + ", ";
        s += "]";

        s += ">";
        return s;
    }
}
