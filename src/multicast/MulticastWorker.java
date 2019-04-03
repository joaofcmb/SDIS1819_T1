package multicast;

import peer.Peer;
import storage.StorageManager;

import java.io.IOException;
import java.util.Random;

public class MulticastWorker implements Runnable {
    private String[] header;
    private String body;

    MulticastWorker(String[][] msg) {
        this.header = msg[0];
        this.body = msg[1] != null ? msg[1][0] : null;
    }

    @Override
    public void run() {
        try {
            switch(header[0]) {
                case "PUTCHUNK":
                    StorageManager.storeChunk(header[3], Integer.parseInt(header[4]), Integer.parseInt(header[5]), body);
                    Thread.sleep(new Random().nextInt(401));
                    Peer.mc.sendMessage(new String[]{"STORED", header[1], header[2], header[3], header[4]});
                    break;
                case "STORED":
                    StorageManager.signalStoreChunk(header[3], Integer.parseInt(header[4]));
                    break;
            }
        } catch (InterruptedException | IOException e) {
            System.err.println("WARNING: Message Discarded: " + header.toString());
        }
    }
}
