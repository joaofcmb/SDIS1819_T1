package multicast;

import peer.Peer;
import storage.StorageManager;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

// TODO export chunkRequests functionality to seperate class

public class MulticastWorker implements Runnable {
    private static final ConcurrentHashMap<String, byte[]> chunkRequests = new ConcurrentHashMap<>();

    private final int waitTime = new Random().nextInt(401);

    private String[] header;
    private byte[] body;

    MulticastWorker(AbstractMap.SimpleImmutableEntry<String[], byte[]> msg) {
        this.header = msg.getKey();
        this.body = msg.getValue();

        System.out.println("->" + Arrays.toString(header));
    }

    public static byte[] retrieveChunk(String fileId, int chunkNo) {
        return chunkRequests.remove(Peer.getId() + fileId + String.valueOf(chunkNo));
    }

    @Override
    public void run() {
        try {
            switch(header[0]) {
                case "PUTCHUNK":
                    if (header[2].equals(Peer.getId())) break;

                    System.out.println(String.join("|", header));

                    StorageManager.storeChunk(header[3], Integer.parseInt(header[4]), Integer.parseInt(header[5]), body);
                    Thread.sleep(waitTime);
                    Peer.mc.sendMessage(new String[]{"STORED", header[1], header[2], header[3], header[4]});
                    break;

                case "STORED":
                    StorageManager.signalStoreChunk(header[3], Integer.parseInt(header[4]));
                    break;

                case "GETCHUNK":
                    int chunkNo = Integer.parseInt(header[4]);

                    if (StorageManager.hasChunk(header[3], chunkNo)) {
                        String key = header[2] + header[3] + header[4];
                        chunkRequests.put(key, null);

                        Thread.sleep(waitTime);

                        if (chunkRequests.containsKey(key)) {
                            chunkRequests.remove(key);

                            Peer.mdr.sendMessage(header, StorageManager.getChunk(header[3], chunkNo));
                        }
                    }
                    break;

                case "CHUNK":
                    String key = header[2] + header[3] + header[4];

                    if (chunkRequests.containsKey(key)) // Use map to avoid duplicate CHUNK messages
                        chunkRequests.remove(key);
                    else if (header[2].equals(Peer.getId())) // Use map to store received chunks
                        chunkRequests.put(key, body);
                    break;
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("Message Discarded: " + String.join("|", header));
        }
    }
}
