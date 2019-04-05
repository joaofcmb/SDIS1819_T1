package peer;

import multicast.MulticastWorker;

import java.util.concurrent.Callable;

public class RestoreWorker implements Callable<byte[]> {
    private final int WAIT_TIME = 2000;

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
        Peer.mc.sendMessage(new String[] {"GETCHUNK", protocolVersion, id, fileId, String.valueOf(chunkNo)} );

        try {
            Thread.sleep(WAIT_TIME);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return MulticastWorker.retrieveChunk(fileId, chunkNo);
    }
}
