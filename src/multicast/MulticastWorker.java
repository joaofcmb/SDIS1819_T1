package multicast;

import peer.Peer;
import storage.ChunkInfo;
import storage.RestoreManager;
import storage.StorageManager;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class MulticastWorker implements Runnable {
    private static final ConcurrentHashMap<String, Object> flagMap = new ConcurrentHashMap<>();

    private final int waitTime = new Random().nextInt(401);

    private String[] header;
    private byte[] body;

    MulticastWorker(AbstractMap.SimpleImmutableEntry<String[], byte[]> msg) {
        this.header = msg.getKey();
        this.body = msg.getValue();

        //System.out.println("->" + Arrays.toString(header));
    }

    @Override
    public void run() {
        try {
            int chunkNo = 0;
            if (header.length > 4)
                chunkNo = Integer.parseInt(header[4]);

            switch(header[0]) {
                case "PUTCHUNK":
                    flagMap.remove(header[3] + chunkNo);

                    if (header[2].equals(Peer.getId())) break;

                    if (StorageManager.storeChunk(header[3], chunkNo, Integer.parseInt(header[5]), body)) {
                        Thread.sleep(waitTime);

                        header[0] = "STORED";
                        header[1] = Peer.getProtocolVersion();
                        header[2] = Peer.getId();
                        Peer.mc.sendMessage(header);
                    }
                    break;
                case "STORED":
                    StorageManager.signalStoreChunk(header[3], chunkNo);
                    break;
                case "GETCHUNK":
                    if (StorageManager.hasChunk(header[3], chunkNo)) {
                        RestoreManager.markChunk(header[3], chunkNo);

                        Thread.sleep(waitTime);

                        if (RestoreManager.checkAndUnMarkChunk(header[3], chunkNo)) {
                            header[0] = "CHUNK";
                            header[1] = Peer.getProtocolVersion();
                            header[2] = Peer.getId();
                            Peer.mdr.sendMessage(header, StorageManager.getChunk(header[3], chunkNo));
                        }
                    }
                    break;
                case "DELETE":
                    StorageManager.deleteChunks(header[3]);
                case "CHUNK":
                    RestoreManager.unMarkChunk(header[3], chunkNo);
                    RestoreManager.putChunk(header[3], chunkNo, body);
                    break;
                case "REMOVED":
                    ChunkInfo chunkInfo = StorageManager.signalRemoveChunk(header[3], chunkNo);

                    if (chunkInfo != null) {
                        flagMap.putIfAbsent(header[3] + chunkNo, new Object());

                        Thread.sleep(waitTime);

                        if (flagMap.remove(header[3] + chunkNo) != null) {
                            Peer.mdb.sendMessage(new String[]{"PUTCHUNK", Peer.getProtocolVersion(), Peer.getId(),
                                            header[3], String.valueOf(chunkNo),
                                            String.valueOf(chunkInfo.getReplicationDegree())
                                    },
                                    chunkInfo.getChunk()
                            );
                        }
                    }
                    break;
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("Message Discarded: " + String.join("|", header));
        }
    }
}
