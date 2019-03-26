package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class StorageManager {
    private static final int CHUNK_SIZE = 64000;

    public static byte[][] fileToChunks(String path) throws IOException {
        File file = new File(path);
        FileInputStream fileInput = new FileInputStream(file);

        byte[][] chunks = new byte[Math.toIntExact(file.length() / CHUNK_SIZE + 1)][CHUNK_SIZE];

        for (byte[] chunk : chunks)
            fileInput.read(chunk);

        return chunks;
    }

}
