package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * Class containing the information related to a file backed up by this peer
 *
 */
public class FileInfo {
    private static final int CHUNK_SIZE = 64000;

    private final int chunkNum; // Number of chunks
    private final int replicationDegree;
    private final AtomicIntegerArray replication;
    private final File file;

    /**
     * Constructor initializing the file information
     *
     * @param file File class of the file
     * @param replicationDegree Desired replication of the file
     */
    public FileInfo(File file, int replicationDegree) {
        this.chunkNum = Math.toIntExact(file.length() / CHUNK_SIZE + 1);
        this.replicationDegree = replicationDegree;
        this.replication = new AtomicIntegerArray(chunkNum);
        this.file = file;
    }

    /**
     * Increments the perceived replication of a chunk
     *
     * @param chunkNo Id of the chunk
     */
    public void incReplication(int chunkNo) {
        if (chunkNo < this.replication.length()) {
            this.replication.incrementAndGet(chunkNo);
        }
    }

    /**
     * Decrements the perceived replication of a chunk
     *
     * @param chunkNo Id of the chunk
     */
    public void decReplication(int chunkNo) {
        if (chunkNo < this.replication.length()) {
            this.replication.decrementAndGet(chunkNo);
        }
    }

    /**
     * Seperates the file into chunks to be backed up individually
     *
     * @return List of chunks
     *
     * @throws IOException on failure to read the file
     */
    public byte[][] retrieveChunks() throws IOException {
        byte[][] chunks = new byte[chunkNum][CHUNK_SIZE];
        chunks[chunkNum - 1] = new byte[Math.toIntExact(file.length() % CHUNK_SIZE)];

        try (FileInputStream fis = new FileInputStream(file)) {
            for (byte[] chunk : chunks)
                fis.read(chunk);
        }

        return chunks;
    }

    /**
     * Retrieves the perceived replication of a chunk
     *
     * @param chunkNo Id of the chunk
     *
     * @return Perceived replication of the chunk
     */
    public int getReplication(int chunkNo) {
        return this.replication.get(chunkNo);
    }

    public int getChunkNum() {
        return chunkNum;
    }

    public int getReplicationDegree() {
        return replicationDegree;
    }
}
