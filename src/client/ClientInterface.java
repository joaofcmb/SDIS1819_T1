package client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientInterface extends Remote {
    boolean backup(String path, int replicationDegree) throws RemoteException;
    boolean restore(String path) throws RemoteException;
    boolean delete(String path) throws RemoteException;
    boolean reclaim(double diskSpace) throws RemoteException;
    String state() throws RemoteException;
}