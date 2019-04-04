package storage;

import peer.Peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ChunkInfo {
    private final File chunkFile;
    private final File infoFile;

    public ChunkInfo(String fileId, int chunkNo, int replicationDegree, String body) throws IOException {
        this.chunkFile = new File("./peer" + Peer.getId() + "/backup/" + fileId + "/chk" + chunkNo);
        this.chunkFile.getParentFile().mkdirs();
        this.chunkFile.createNewFile();

        this.infoFile = new File("./peer" + Peer.getId() + "/info/" + fileId + "/chk" + chunkNo);
        this.infoFile.getParentFile().mkdirs();
        this.infoFile.createNewFile();

        FileOutputStream out = new FileOutputStream(chunkFile);
        out.write(body.getBytes());
        out.close();

        synchronized (infoFile) {
            // The file keeps track of necessary replication to fulfill replicationDegree (<=0 means it's fulfilled)
            try (PrintWriter pw = new PrintWriter(infoFile)) {
                pw.println(replicationDegree);
            }
        }
    }

    public void incReplication() throws IOException {
        synchronized (infoFile) {
            int necessaryReplication;

            try (Scanner s = new Scanner(infoFile)) {
                necessaryReplication = s.nextInt();
            }

            try (PrintWriter pw = new PrintWriter(infoFile)) {
                pw.println(necessaryReplication - 1);
            }
        }
    }
}
