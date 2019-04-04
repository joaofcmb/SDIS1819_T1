package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileInfo {
    private static final int CHUNK_SIZE = 64000;

    private final byte[][] chunks;
    private int[] replication;

    public FileInfo(File file) throws IOException {
        int chunkNum = Math.toIntExact(file.length() / CHUNK_SIZE + 1);

        this.chunks = new byte[chunkNum][CHUNK_SIZE];
        this.chunks[chunkNum - 1] = new byte[Math.toIntExact(file.length() % CHUNK_SIZE)];
        this.replication = new int[chunkNum];

        try (FileInputStream fis = new FileInputStream(file)) {
            for (byte[] chunk : this.chunks)
                fis.read(chunk);
        }
    }

    public void incReplication(int chunkNo) {
        if (chunkNo < this.replication.length)
            this.replication[chunkNo]++;
    }

    public byte[][] getChunks() {
        return chunks;
    }

    public int getReplication(int chunkNo) {
        return this.replication[chunkNo];
    }
}
