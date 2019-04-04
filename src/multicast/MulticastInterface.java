package multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.AbstractMap;

public class MulticastInterface {
    private InetAddress group;
    private int port;
    private MulticastSocket s;

    private static final char CR = 0x0D;
    private static final char LF = 0x0A;

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

    public synchronized void sendMessage(String[] header) {
        byte[] msg = (String.join(" ", header) + CR + LF + CR + LF).getBytes();

        try {
            s.send(new DatagramPacket(msg, msg.length, group, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

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

    public AbstractMap.SimpleImmutableEntry<String[], byte[]> receiveMessage() {
        byte[] buf = new byte[64064];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            s.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());

        return decodeMessage(packet.getData(), packet.getLength());
    }

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
