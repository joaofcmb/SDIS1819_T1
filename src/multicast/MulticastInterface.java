package multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.AbstractMap;

/**
 * Class implementing an interface for a multicast channel, wrapping the details of the message format.
 */
public class MulticastInterface {
    private InetAddress group;
    private int port;
    private MulticastSocket s;

    private static final char CR = 0x0D;
    private static final char LF = 0x0A;

    /**
     * Constructor creating an interface for a multicast channel given its address
     *
     * @param address IPV4 address of the multicast channel
     * @param port Port of the multicast channel
     */
    public MulticastInterface(String address, int port) {
        try {
            this.group = InetAddress.getByName(address);
            this.port = port;

            this.s = new MulticastSocket(port);
            this.s.joinGroup(group);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message with a single line header through the multicast channel
     *
     * @param header Header fields to be sent
     */
    public synchronized void sendMessage(String[] header) {
        byte[] msg = (String.join(" ", header) + CR + LF + CR + LF).getBytes();

        try {
            s.send(new DatagramPacket(msg, msg.length, group, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message with a single line header and a body through the multicast channel
     *
     * @param header Header fields to be sent
     * @param body Body contents to be sent
     */
    public synchronized void sendMessage(String[] header, byte[] body) {
        byte[] headerMsg = (String.join(" ", header) + CR + LF + CR + LF).getBytes();

        byte[] msg = new byte[headerMsg.length + body.length];
        System.arraycopy(headerMsg, 0, msg,0, headerMsg.length);
        System.arraycopy(body, 0, msg, headerMsg.length, body.length);

        try {
            s.send(new DatagramPacket(msg, msg.length, group, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receives a message from the multicast channel
     *
     * @return Pair containing the header fields and the body. The body is null in case there is no body in the message
     */
    public AbstractMap.SimpleImmutableEntry<String[], byte[]> receiveMessage() {
        byte[] buf = new byte[65000];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            s.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return decodeMessage(packet.getData(), packet.getLength());
    }

    /**
     * Helper method for decoding the data of a message
     *
     * @param data raw data of the received data packet
     * @param length length of the raw data
     * @return Pair containing the header fields and the body. The body is null in case there is no body in the message
     */
    private AbstractMap.SimpleImmutableEntry<String[], byte[]> decodeMessage(byte[] data, int length) {
        for (int i = 0; i < length - 4; i++) {
            if (data[i] == CR && data[i+1] == LF && data[i+2] == CR && data[i+3] == LF) {
                byte[] header = new byte[i];
                byte[] body = new byte[length - i - 4];

                System.arraycopy(data, 0, header, 0, header.length);
                System.arraycopy(data, i + 4, body, 0, body.length);

                return new AbstractMap.SimpleImmutableEntry<>(
                        new String(header).split(" +"),
                        body
                );
            }

        }

        return new AbstractMap.SimpleImmutableEntry<>(
                new String(data, 0, length - 4).split(" +"),
                null
        );
    }
}
