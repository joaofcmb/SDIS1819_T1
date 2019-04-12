package peer;

import client.ClientInterface;
import storage.ChunkInfo;
import storage.StorageManager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

// TODO use exception throwing on RMI for better error display for client

public class Service implements ClientInterface {
    @Override
    public boolean backup(String path, int replicationDegree) {
        try {
            System.out.println("BACKUP COMMAND: " + path + " " + replicationDegree);

            if (StorageManager.isBackedUp(path))
                this.delete(path);

            String fileId = StorageManager.generateFileId(path, replicationDegree);

            int chunkNo = 0;
            LinkedList<Callable<Boolean>> workers = new LinkedList<>();
            for (byte[] chunk : StorageManager.retrieveFileChunks(fileId))
                workers.add(new BackupWorker(fileId, chunk, chunkNo++, replicationDegree));

            List<Future<Boolean>> resultList = Peer.getBackupThreadPool().invokeAll(workers);

            for (Future<Boolean> result : resultList) {
                if (!result.get()) throw new Exception();
            }

            System.out.println("Backup protocol for \"" + path + "\" successful.");
            return true;
        } catch (Exception e) {
            System.out.println("WARNING: Backup protocol ended. Replication degree not fully met.");
            return false;
        }
    }

    @Override
    public boolean restore(String path) {
        System.out.println("RESTORE COMMAND: " + path);

        try {
            String fileId = StorageManager.getFileId(path);
            if (fileId == null)
                throw new FileNotFoundException();

            int chunkNum = StorageManager.getChunkNum(fileId);

            LinkedList<Callable<byte[]>> workers = new LinkedList<>();
            for (int chunkNo = 0; chunkNo < chunkNum; chunkNo++)
                workers.add(new RestoreWorker(fileId, chunkNo));

            List<Future<byte[]>> resultList = Peer.getRestoreThreadPool().invokeAll(workers);

            byte[][] chunks = new byte[chunkNum][];
            for (int i = 0; i < chunkNum; i++) {
                if (!resultList.get(i).isDone()) throw new Exception();

                chunks[i] = resultList.get(i).get();
            }

            StorageManager.restoreFile(path, chunks);

            System.out.println("Restore protocol for \"" + path + "\" successful.");
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: Restore protocol failed. The Operation wasn't fully successful");
            return false;
        }
    }

    @Override
    public boolean delete(String path) {
        System.out.println("DELETE COMMAND: " + path);

        try {
            String fileId = StorageManager.deleteFile(path);
            if (fileId == null)
                throw new FileNotFoundException();

            Peer.mc.sendMessage(new String[] {"DELETE", Peer.getProtocolVersion(), Peer.getId(), fileId} );

            System.out.println("Delete protocol for \"" + path + "\" successful.");
            return true;
        } catch (Exception e) {
            System.out.println("ERROR: Deletion protocol failed.");
            return false;
        }
    }

    @Override
    public boolean reclaim(double diskSpace) {
        System.out.println("RECLAIM COMMAND: " + diskSpace);

        try {
            StorageManager.reclaimSpace(diskSpace);

            System.out.println("Reclaim protocol successful (" + diskSpace + " max storage).");
            return true;
        } catch (Exception e) {
            System.out.println("ERROR: Reclaim protocol failed.");
            return false;
        }
    }

    @Override
    public String state() {
        System.out.println("STATE COMMAND");

        try {
            System.out.println("State retrieval successful");
            return StorageManager.getState();
        } catch (IOException e) {
            System.out.println("ERROR: State retrieval failed.");
            return null;
        }
    }

}