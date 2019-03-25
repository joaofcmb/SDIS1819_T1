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

    public void sendMessage(String[] header) {
        String msg = String.join(" ", header) + CR + LF + CR + LF;

        try {
            s.send(new DatagramPacket(msg.getBytes(), msg.length(), group, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String[] header, String body) {
        String msg = String.join(" ", header) + CR + LF + CR + LF + body;

        try {
            s.send(new DatagramPacket(msg.getBytes(), msg.length(), group, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String receiveMessage(String[] header) {
        byte[] buf = new byte[64];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            s.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());

        String[] msgLines = msg.split("" + CR + LF);

        header = msgLines[0].split(" +");

        if (msgLines.length > 1)
            return msgLines[msgLines.length - 1];
        else
            return null;
    }
}
