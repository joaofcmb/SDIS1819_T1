package multicast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

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

    public String[][] receiveMessage() {
        byte[] buf = new byte[64];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            s.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());

        String[] msgLines = msg.split("" + CR + LF);

        return new String[][] {
                msgLines[0].split(" +"),                                             // header
                msgLines.length > 1 ? new String[]{msgLines[msgLines.length - 1]} : null   // body
        };
    }
}
