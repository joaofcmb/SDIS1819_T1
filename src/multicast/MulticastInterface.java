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

        System.out.println("SEND: " + new String(msg));

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

        System.out.println("SEND: " + new String(msg) + "X");

        try {
            s.send(new DatagramPacket(msg, msg.length, group, port));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String[][] receiveMessage() {
        byte[] buf = new byte[64064];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);
        try {
            s.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());

        System.out.println("RECV: " + msg + "X");

        String[] msgLines = msg.split("" + CR + LF + CR + LF, 2);

        for (String line : msgLines)
            System.out.println("|" + line + "|");

        String[] header = msgLines[0].split(" +");
        String body = msgLines.length > 1 ? msgLines[1] : null;

        return new String[][] { header, new String[]{body}};
    }
}
