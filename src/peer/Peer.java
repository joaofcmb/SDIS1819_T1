package peer;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

import client.ClientInterface;

public class Peer {
    private static String protocolVersion;
    private static String ID;
    private static String accessPoint;

    public static void main(String[] args) {
        Peer.initialize(args);

        try {
            CommandExecutor commandExecutor = new CommandExecutor();
            ClientInterface stub = (ClientInterface) UnicastRemoteObject.exportObject(commandExecutor, 0);
            LocateRegistry.getRegistry().bind(Peer.accessPoint, stub);
        } catch (Exception e) {
            System.err.println("Peer(" + ID + ") - RMI exception: " + e.toString());
            e.printStackTrace();
        }

        System.out.println("Peer(" + ID + ") - RMI Done");
    }

    private static void initialize(String[] args) throws IllegalArgumentException {
        if (args.length != 3 && args.length != 9)
            throw new IllegalArgumentException();

        Peer.protocolVersion = args[0];
        Peer.ID = args[1];
        Peer.accessPoint = args[2];
    }
}