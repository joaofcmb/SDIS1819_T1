package peer;

import client.ClientInterface;
import storage.StorageManager;

public class Service implements ClientInterface {
    @Override
    public boolean backup(String path, int replicationDegree) {
        try {
            System.out.println("BACKUP COMMAND: " + path + " " + replicationDegree);

            byte[] fileId = StorageManager.fileId(path);

            int chunkNo = 0;
            for (byte[] chunk : StorageManager.fileToChunks(fileId))
                Peer.getProtocolThreadPool().execute(new BackupWorker(fileId, chunk, chunkNo++, replicationDegree));

            // TODO check if all chunks were backed up. If not cancel the operation

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