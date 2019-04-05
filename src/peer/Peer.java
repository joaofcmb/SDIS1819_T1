package peer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import client.ClientInterface;
import multicast.MulticastInterface;
import multicast.MulticastThread;
import storage.StorageManager;

public class Peer {
    private static String protocolVersion;
    private static String id;
    private static String accessPoint;

    public static MulticastInterface mc, mdb, mdr;

    private static final ThreadPoolExecutor backupThreadPool = new ThreadPoolExecutor(
            5, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new SynchronousQueue<>());

    private static final ThreadPoolExecutor restoreThreadPool = new ThreadPoolExecutor(
            5, Integer.MAX_VALUE, 5, TimeUnit.SECONDS, new SynchronousQueue<>());

    private static final ThreadPoolExecutor multicastThreadPool = new ThreadPoolExecutor(
            8, 8, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

    public static void main(String[] args) throws RemoteException {
        if (args.length != 9)
            throw new IllegalArgumentException();

        initPeerInfo(args);
        StorageManager.storageSetup();
        initRMI();
        initMulticast(args);

        System.out.println("Peer(" + id + ") online.");
    }

    private static void initPeerInfo(String[] args) throws IllegalArgumentException {
        Peer.protocolVersion = args[0];
        Peer.id = args[1];
        Peer.accessPoint = args[2];
    }

    private static void initRMI() throws RemoteException {
        Service service = new Service();
        ClientInterface stub = (ClientInterface) UnicastRemoteObject.exportObject(service, 0);
        LocateRegistry.getRegistry().rebind(Peer.accessPoint, stub);
    }


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

    public static ThreadPoolExecutor getBackupThreadPool() {
        return backupThreadPool;
    }

    public static ThreadPoolExecutor getMulticastThreadPool() {
        return multicastThreadPool;
    }

    public static ThreadPoolExecutor getRestoreThreadPool() {
        return restoreThreadPool;
    }
}