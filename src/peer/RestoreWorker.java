package peer;

import storage.RestoreManager;

import java.util.concurrent.Callable;

public class RestoreWorker implements Callable<byte[]> {
    private final int INIT_WAIT_TIME = 1000;

    private final String protocolVersion = Peer.getProtocolVersion();
    private final String id = Peer.getId();

    private final String fileId;
    private final int chunkNo;

    public RestoreWorker(String fileId, int chunkNo) {
        this.fileId = fileId;
        this.chunkNo = chunkNo;
    }

    @Override
    public byte[] call() {
        int waitTime = INIT_WAIT_TIME;
        byte[] chunk = null;

        Peer.mc.sendMessage(new String[] {"GETCHUNK", protocolVersion, id, fileId, String.valueOf(chunkNo)} );

        while(chunk == null) {
            RestoreManager.addChunk(fileId, chunkNo);

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            chunk = RestoreManager.retrieveChunk(fileId, chunkNo);
            waitTime *= 2;
        }

        return chunk;
    }
}
