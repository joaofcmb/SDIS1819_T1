package peer;

public class BackupWorker implements Runnable {
    private final int INIT_WAIT_TIME = 1000, TIMEOUT_THRESHOLD = 5;

    private final String protocolVersion = Peer.getProtocolVersion();
    private final String id = Peer.getId();

    private final byte[] fileId, chunk;
    private final int chunkNo, replicationDegree;

    BackupWorker(byte[] fileId, byte[] chunk, int chunkNo, int replicationDegree) {
        this.fileId = fileId;
        this.chunk = chunk;
        this.chunkNo = chunkNo;
        this.replicationDegree = replicationDegree;
    }

    @Override
    public void run() {
        int waitTime = INIT_WAIT_TIME, timeoutCounter = 0;
        String[] header = new String[]{"PUTCHUNK", protocolVersion, id, new String(fileId),
                String.valueOf(chunkNo), String.valueOf(replicationDegree)};
        do {
            Peer.mdb.sendMessage(header, chunk);

            try {
                Thread.sleep(INIT_WAIT_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // TODO check if replicationDegree was met and terminate properly in that case

            timeoutCounter *= 2;
        } while(timeoutCounter < TIMEOUT_THRESHOLD);
    }
}
