package storage;

import peer.Peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Class containing the information related to a chunk stored on the peer
 *
 * It is responsible for accessing the content and updating it.
 */
public class ChunkInfo implements Comparable<ChunkInfo> {
    private final File chunkFile;
    private final File infoFile;

    private final int replicationDegree;
    private int redundancy; // Represents the difference between the perceived and desired replication of the chunk

    /**
     * Constructor handling the initial information of a chunk and storing it appropriately
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     * @param replication Perceived replication of the chunk
     * @param replicationDegree Desired replication of the chunk
     * @param body Contents of the chunk
     *
     * @throws IOException on failure to write data on disk
     */
    public ChunkInfo(String fileId, int chunkNo, int replication, int replicationDegree, byte[] body) throws IOException {
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
                pw.println(replication + " " + replicationDegree);
            }

            this.replicationDegree = replicationDegree;
            this.setRedundancy(replication - replicationDegree);
        }
    }

    /**
     * Increments the perceived replication of this chunk
     *
     * @throws IOException on failure to update data on disk
     */
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

    /**
     * Decrements the perceived replication of this chunk
     *
     * @throws IOException on failure to update data on disk
     */
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

    /**
     * Retrieves the contents of this chunk
     *
     * @return Binary content of the chunk
     *
     * @throws IOException on failure to read the data from disk
     */
    public byte[] getChunk() throws IOException {
        synchronized (chunkFile) {
            try (FileInputStream fis = new FileInputStream(chunkFile)) {
                byte[] body = new byte[Math.toIntExact(chunkFile.length())];
                fis.read(body);
                return body;
            }
        }
    }

    /**
     * Deletes the data of this chunk on disk
     */
    public void delete() {
        this.chunkFile.delete();
        this.infoFile.delete();
    }

    /**
     * Retrieves the perceived replication of this chunk
     *
     * @return Perceived replication of the chunk
     *
     * @throws IOException on failure to read the data from disk
     */
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

    /**
     * Compares two stored chunks to determine the deletion priority during a storage reclaim
     *
     * @param o ChunkInfo being compared
     *
     * @return Comparison between this and the target ChunkInfo
     *
     * @see Comparable
     */
    @Override
    public synchronized int compareTo(ChunkInfo o) {
        int r1 = this.getRedundancy(), r2 = o.getRedundancy();

        if      (r1 > r2)   return -1;
        else if (r1 < r2)   return 1;
        else                return Integer.compare(o.replicationDegree, this.replicationDegree);
    }
}
