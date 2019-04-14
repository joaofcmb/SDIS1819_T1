package peer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import client.ClientInterface;
import multicast.MulticastInterface;
import multicast.MulticastThread;
import storage.StorageManager;

/**
 * Class responsible for a Peer.
 *
 * Contains all the important information such as the protocol version, the peer id, the multicast interfaces
 * and the thread pools
 */
public class Peer {
    private static String protocolVersion;
    private static String id;
    private static String accessPoint;

    public static MulticastInterface mc, mdb, mdr;

    private static final ExecutorService backupThreadPool = Executors.newFixedThreadPool(50);

    private static final ExecutorService restoreThreadPool = Executors.newFixedThreadPool(20);

    private static final ExecutorService multicastThreadPool = Executors.newCachedThreadPool();

    /**
     * Main method for the starting up a Peer, configured based on the given command line arguments
     *
     * @param args command line arguments used to define the peer's configuration
     * @throws RemoteException on RMI failure
     */
    public static void main(String[] args) throws RemoteException {
        if (args.length != 9)
            throw new IllegalArgumentException();

        initPeerInfo(args);
        initRMI();
        initMulticast(args);
        StorageManager.storageSetup();

        System.out.println("Peer(" + id + ") online.");
    }

    /**
     * Initiates the peer's protocol version, id and access point for RMI
     *
     * @param args command line arguments used to define the peer's configuration
     */
    private static void initPeerInfo(String[] args) {
        Peer.protocolVersion = args[0];
        Peer.id = args[1];
        Peer.accessPoint = args[2];
    }

    /**
     * Initiates the peer RMI stub
     *
     * @throws RemoteException on RMI failure
     */
    private static void initRMI() throws RemoteException {
        Service service = new Service();
        ClientInterface stub = (ClientInterface) UnicastRemoteObject.exportObject(service, 0);
        LocateRegistry.getRegistry().rebind(Peer.accessPoint, stub);
    }

    /**
     * Initiates the Multicast Threads based on the input configuration
     *
     * @param args command line arguments used to define the peer's configuration
     */
    private static void initMulticast(String[] args) {
        mc = new MulticastInterface(args[3], Integer.parseInt(args[4]));
        mdb = new MulticastInterface(args[5], Integer.parseInt(args[6]));
        mdr = new MulticastInterface(args[7], Integer.parseInt(args[8]));

        new MulticastThread(mc).start();
        new MulticastThread(mdb).start();
        new MulticastThread(mdr).start();
    }

    public static String getProtocolVersion() {
        return protocolVersion;
    }

    public static String getId() {
        return id;
    }

    public static ExecutorService getBackupThreadPool() {
        return backupThreadPool;
    }

    public static ExecutorService getMulticastThreadPool() {
        return multicastThreadPool;
    }

    public static ExecutorService getRestoreThreadPool() {
        return restoreThreadPool;
    }
}