package util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class NetUtil {
    private static final boolean DEBUG = false;

    private static ByteBuffer readBytes(SocketChannel sc, int len) {
        ByteBuffer buf = ByteBuffer.allocate(len);

        int bytesRead = 0;
        while (bytesRead != len) {
            int r = -1;
            try {
                r = sc.read(buf);
            } catch (IOException e) {
                r = -1;
            }
            if (r == -1) {
                try {
                    sc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                return null;
            }

            bytesRead += r;
        }

        return buf;
    }

    public static byte[] recv(SocketChannel sc) {
        ByteBuffer lenBuf = readBytes(sc, Integer.SIZE / 8);
        ByteBuffer dataBuf = null;
        int len = 0;

        if (lenBuf != null) {
            lenBuf.flip();

            if (DEBUG) {
                System.out.println("RECEIVING " + (Integer.SIZE / 8)
                        + " bytes from " + sc + ": "
                        + Arrays.toString(lenBuf.array()));
            }

            len = lenBuf.getInt();
            dataBuf = readBytes(sc, len);
            dataBuf.flip();

            if (DEBUG) {
                System.out.println("RECEIVING " + len + " bytes from " + sc
                        + ": " + Arrays.toString(dataBuf.array()));
            }

            if (DEBUG) {
                System.out.println("Has received a message of size "
                        + dataBuf.array().length + " from " + sc);
            }

            return dataBuf.array();
        }

        return null;
    }

    private static void sendBytes(byte[] msg, SocketChannel sc) {
        ByteBuffer buf = ByteBuffer.wrap(msg);

        if (DEBUG) {
            System.out.println("SENDING " + msg.length + " bytes to " + sc
                    + ": " + Arrays.toString(msg));
        }

        if (sc != null) {
            try {
                while (buf.remaining() > 0) {
                    sc.write(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            System.out.println("Cannot send a message: I don't have a SocketChannel");
        }
    }

    public static int sendMessage(byte[] msg, SocketChannel sc) {
        if (DEBUG) {
            System.out.println("Sending a message of size " + msg.length
                    + " to " + sc);
        }

        ByteBuffer b = ByteBuffer.allocate(Integer.SIZE / 8);
        b.putInt(msg.length);
        b.flip();
        sendBytes(b.array(), sc);
        sendBytes(msg, sc);

        return msg.length + Integer.SIZE / 8;
    }
}
