package peer;

import storage.StorageManager;

import java.util.concurrent.Callable;

/**
 * Worker of the Backup protocol, responsible for the backup of a single chunk
 */
public class BackupWorker implements Callable<Boolean> {
    private final int INIT_WAIT_TIME = 1000, TIMEOUT_THRESHOLD = 5;

    private final String protocolVersion = Peer.getProtocolVersion();
    private final String id = Peer.getId();

    private final String fileId;
    private final byte[] chunk;
    private final int chunkNo, replicationDegree;

    /**
     * Constructor of the chunk backup worker, initializing it with the needed values
     *
     * @param fileId Id of the chunk's file
     * @param chunk Contents of the chunk
     * @param chunkNo Id of the chunk
     * @param replicationDegree Desired replication degree of the chunk
     */
    BackupWorker(String fileId, byte[] chunk, int chunkNo, int replicationDegree) {
        this.fileId = fileId;
        this.chunk = chunk;
        this.chunkNo = chunkNo;
        this.replicationDegree = replicationDegree;
    }

    /**
     * Method implementing the actions performed by the worker to execute the chunk backup protocol
     *
     * @return Boolean representing whether the chunk backup was successful or not
     */
    @Override
    public Boolean call() {
        int waitTime = INIT_WAIT_TIME, timeoutCounter = 0;
        String[] header = new String[]{"PUTCHUNK", protocolVersion, id, fileId,
                String.valueOf(chunkNo), String.valueOf(replicationDegree)};
        do {
            Peer.mdb.sendMessage(header, chunk);

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (StorageManager.getChunkReplication(fileId, chunkNo) >= this.replicationDegree)
                return true;

            waitTime *= 2;
        } while(++timeoutCounter < TIMEOUT_THRESHOLD);

        return false;
    }
}
