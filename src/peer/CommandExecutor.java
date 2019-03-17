package peer;

import java.rmi.RemoteException;

import client.ClientInterface;

public class CommandExecutor implements ClientInterface {
    @Override
    public boolean backup(String path, int replicationDegree) throws RemoteException {
        System.out.println("BACKUP COMMAND: " + path + " " + replicationDegree);
        return false;
    }

    @Override
    public boolean restore(String path) throws RemoteException {
        System.out.println("RESTORE COMMAND: " + path);
        return false;
    }

    @Override
    public boolean delete(String path) throws RemoteException {
        System.out.println("DELETE COMMAND: " + path);
        return false;
    }

    @Override
    public boolean reclaim(int diskSpace) throws RemoteException {
        System.out.println("RECLAIM COMMAND: " + diskSpace);
        return false;
    }

    @Override
    public boolean state() throws RemoteException {
        System.out.println("STATE COMMAND");
        return false;
    }

}