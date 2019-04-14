package peer;

import storage.RestoreManager;

import java.util.concurrent.Callable;

/**
 * Worker of the Restore protocol, responsible for the restore of a single chunk
 */
public class RestoreWorker implements Callable<byte[]> {
    private final int INIT_WAIT_TIME = 1000;

    private final String protocolVersion = Peer.getProtocolVersion();
    private final String id = Peer.getId();

    private final String fileId;
    private final int chunkNo;

    /**
     * Constructor of the chunk restore worker, initializing it with the needed values
     *
     * @param fileId Id of the chunk's file
     * @param chunkNo Id of the chunk
     */
    public RestoreWorker(String fileId, int chunkNo) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    /**
     * Method implementing the actions performed by the worker to execute the chunk restore protocol
     *
     * @return Binary content of the restored chunk.
     */
    @Override
    public byte[] call() {
        int waitTime = INIT_WAIT_TIME;
        byte[] chunk = null;

        Peer.mc.sendMessage(new String[] {"GETCHUNK", protocolVersion, id, fileId, String.valueOf(chunkNo)} );

        while(chunk == null && waitTime < 32000) {
            RestoreManager.addChunk(fileId, chunkNo);

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            chunk = RestoreManager.retrieveChunk(fileId, chunkNo);
            waitTime *= 2;
        }

        if (chunk == null)
            Thread.currentThread().interrupt();

        return chunk;
    }
}
