package multicast;

import peer.Peer;
import storage.StorageManager;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Random;

public class MulticastWorker implements Runnable {
    private String[] header;
    private byte[] body;

    MulticastWorker(AbstractMap.SimpleImmutableEntry<String[], byte[]> msg) {
        this.header = msg.getKey();
        this.body = msg.getValue();

        System.out.println("->" + Arrays.toString(header));
    }

    @Override
    public void run() {
        try {
            switch(header[0]) {
                case "PUTCHUNK":
                    if (header[2].equals(Peer.getId())) break;

                    System.out.println(String.join("|", header));

                    StorageManager.storeChunk(header[3], Integer.parseInt(header[4]), Integer.parseInt(header[5]), body);
                    Thread.sleep(new Random().nextInt(401));
                    Peer.mc.sendMessage(new String[]{"STORED", header[1], header[2], header[3], header[4]});
                    break;
                case "STORED":
                    StorageManager.signalStoreChunk(header[3], Integer.parseInt(header[4]));
                    break;
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("Message Discarded: " + String.join("|", header));
        }
    }
}
