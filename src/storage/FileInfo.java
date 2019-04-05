package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicIntegerArray;

public class FileInfo {
    private static final int CHUNK_SIZE = 64000;
    private final int chunkNum;
    private AtomicIntegerArray replication;
    private final File file;

    public FileInfo(File file) {
        this.chunkNum = Math.toIntExact(file.length() / CHUNK_SIZE + 1);
        this.replication = new AtomicIntegerArray(chunkNum);
        this.file = file;
    }

    public void incReplication(int chunkNo) {
        if (chunkNo < this.replication.length()){
            this.replication.incrementAndGet(chunkNo);
        }
    }

    public byte[][] retrieveChunks() throws IOException {
        byte[][] chunks = new byte[chunkNum][CHUNK_SIZE];
        chunks[chunkNum - 1] = new byte[Math.toIntExact(file.length() % CHUNK_SIZE)];

        try (FileInputStream fis = new FileInputStream(file)) {
            for (byte[] chunk : chunks)
                fis.read(chunk);
        }

        return chunks;
    }

    public int getReplication(int chunkNo) {
        return this.replication.get(chunkNo);
    }

    public int getChunkNum() {
        return chunkNum;
    }
}
