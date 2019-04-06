package storage;

import peer.Peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

// TODO keep track of peers who stored chunk to avoid incrementing replication on duplicate stored messages from multiple backups of same file in different peers

public class ChunkInfo {
    private final File chunkFile;
    private final File infoFile;

    public ChunkInfo(String fileId, int chunkNo, int replicationDegree, byte[] body) throws IOException {
        this.chunkFile = new File("./peer" + Peer.getId() + "/backup/" + fileId + "/chk" + chunkNo);
        this.chunkFile.getParentFile().mkdirs();
        this.chunkFile.createNewFile();

        this.infoFile = new File("./peer" + Peer.getId() + "/info/" + fileId + "/chk" + chunkNo);
        this.infoFile.getParentFile().mkdirs();
        this.infoFile.createNewFile();

        synchronized (chunkFile) {
            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                fos.write(body);
            }
        }

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

    public byte[] getChunk() throws IOException {
        synchronized (chunkFile) {
            try (FileInputStream fis = new FileInputStream(chunkFile)) {
                byte[] body = new byte[Math.toIntExact(chunkFile.length())];
                fis.read(body);
                return body;
            }
        }
    }

    public boolean delete() {
        return this.chunkFile.delete() && this.infoFile.delete();
    }
}
