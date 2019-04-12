package storage;

import peer.Peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ChunkInfo implements Comparable<ChunkInfo> {
    private final File chunkFile;
    private final File infoFile;

    private final int replicationDegree;

    private int redundancy;
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
            try (PrintWriter pw = new PrintWriter(infoFile)) {
                pw.println("0 " + replicationDegree);
            }

            this.replicationDegree = replicationDegree;
            this.setRedundancy(replicationDegree);
        }
    }

    public void incReplication() throws IOException {
        synchronized (infoFile) {
            int replication, desired;

            try (Scanner s = new Scanner(infoFile)) {
                replication = s.nextInt() + 1;
                desired = s.nextInt();
            }

            this.setRedundancy(replication - desired);

            try (PrintWriter pw = new PrintWriter(infoFile)) {
                pw.println(replication + " " + desired);
            }
        }
    }

    public void decReplication() throws IOException {
        int replication, desired;

        synchronized (infoFile) {
            try (Scanner s = new Scanner(infoFile)) {
                replication = s.nextInt() - 1;
                desired = s.nextInt();
            }

            this.setRedundancy(replication - desired);

            try (PrintWriter pw = new PrintWriter(infoFile)) {
                pw.println(replication + " " + desired);
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

    public void delete() {
        this.chunkFile.delete();
        this.infoFile.delete();
    }

    public int getReplication() throws IOException {
        synchronized (infoFile) {
            try (Scanner s = new Scanner(infoFile)) {
                return s.nextInt();
            }
        }
    }

    public synchronized int getRedundancy() {
        return redundancy;
    }

    public synchronized void setRedundancy(int redundancy) {
        this.redundancy = redundancy;
    }

    public double getChunkSize() {
        synchronized (chunkFile) {
            return chunkFile.length() / 1000d;
        }
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }

    @Override
    public synchronized int compareTo(ChunkInfo o) {
        int r1 = this.getRedundancy(), r2 = o.getRedundancy();

        if      (r1 > r2)   return -1;
        else if (r2 < r1)   return 1;
        else {
            if      (this.replicationDegree > o.replicationDegree)   return -1;
            else if (this.replicationDegree < o.replicationDegree)   return 1;
            else                                                     return 0;
        }
    }
}
