package multicast;

import peer.Peer;
import storage.RestoreManager;
import storage.StorageManager;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Random;

// TODO export chunkRequests functionality to seperate class

public class MulticastWorker implements Runnable {
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
            int chunkNo = Integer.parseInt(header[4]);

            switch(header[0]) {
                case "PUTCHUNK":
                    if (header[2].equals(Peer.getId())) break;

                    StorageManager.storeChunk(header[3], chunkNo, Integer.parseInt(header[5]), body);

                    Thread.sleep(waitTime);

                    header[0] = "STORED";
                    header[2] = Peer.getId();
                    Peer.mc.sendMessage(header);
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
                            header[2] = Peer.getId();
                            Peer.mdr.sendMessage(header, StorageManager.getChunk(header[3], chunkNo));
                        }
                    }
                    break;

                case "CHUNK":
                    RestoreManager.unMarkChunk(header[3], chunkNo);
                    RestoreManager.putChunk(header[3], chunkNo, body);
                    break;
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("Message Discarded: " + String.join("|", header));
        }
    }
}
