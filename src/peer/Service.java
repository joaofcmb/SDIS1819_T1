package peer;

import client.ClientInterface;
import storage.StorageManager;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class Service implements ClientInterface {
    @Override
    public boolean backup(String path, int replicationDegree) {
        try {
            System.out.println("BACKUP COMMAND: " + path + " " + replicationDegree);

            if (StorageManager.isBackedUp(path))
                this.delete(path);

            String fileId = StorageManager.fileId(path);

            int chunkNo = 0;
            LinkedList<Callable<Boolean>> workers = new LinkedList<>();
            for (byte[] chunk : StorageManager.fileToChunks(fileId))
                workers.add(new BackupWorker(fileId, chunk, chunkNo++, replicationDegree));

            List<Future<Boolean>> resultList = Peer.getProtocolThreadPool().invokeAll(workers);

            for (Future<Boolean> result : resultList)
                if (!result.get()) {
                    System.out.println("ERROR: Backup protocol failed. The Operation was canceled");
                    throw new Exception();
                }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean restore(String path) {
        System.out.println("RESTORE COMMAND: " + path);
        return false;
    }

    @Override
    public boolean delete(String path) {
        System.out.println("DELETE COMMAND: " + path);
        return false;
    }

    @Override
    public boolean reclaim(int diskSpace) {
        System.out.println("RECLAIM COMMAND: " + diskSpace);
        return false;
    }

    @Override
    public boolean state() {
        System.out.println("STATE COMMAND");
        return false;
    }

}