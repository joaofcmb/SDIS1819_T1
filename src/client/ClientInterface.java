package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interface specifying the methods to be implemented by the Server for the RMI interface with the Client
 */
public interface ClientInterface extends Remote {
    /**
     * This Remote Method starts the backup protocol
     *
     * @param path Path of the file in the peer's filesystem to be backed up
     * @param replicationDegree Desired replication degree for the file to be backed up
     * @return Boolean representing if the backup was fully successful or not
     * @throws RemoteException on RMI failure
     */
    boolean backup(String path, int replicationDegree) throws RemoteException;

    /**
     * This remote method starts the restore protocol
     *
     * @param path Path of the previously backed up file to be restored
     * @return Boolean representing if the restore command was fully successful or not
     * @throws RemoteException on RMI failure
     */
    boolean restore(String path) throws RemoteException;

    /**
     * This remote method starts the restore protocol
     *
     * @param path Path of the previously backed up file to be deleted
     * @return Boolean representing if the delete command was fully successful or not
     * @throws RemoteException on RMI failure
     */
    boolean delete(String path) throws RemoteException;

    /**
     * This remote method starts the reclaim protocol
     *
     * @param diskSpace The maximum storage capacity to be used by the peer to backup incoming files in KBytes
     * @return Boolean representing if the reclaim command was fully successful or not
     * @throws RemoteException on RMI failure
     */
    boolean reclaim(double diskSpace) throws RemoteException;

    /**
     * This remote method fetches the peer's current state in a human readable format
     * @return String with the peer's current state in a human readble format
     * @throws RemoteException on RMI failure
     */
    String state() throws RemoteException;
}