package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class StorageManager {
    private static final int CHUNK_SIZE = 64000;

    private static final HashMap<byte[], FileInfo> fileMap = new HashMap<>();

    public static byte[] fileId(String path) throws NoSuchAlgorithmException {
        File file = new File(path);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

        byte[] fileId = sha256.digest((file.getName() + file.length() + file.lastModified()).getBytes(StandardCharsets.UTF_8));

        if (fileMap.get(fileId) == null)
            fileMap.put(fileId, new FileInfo(path));

        return fileId;
    }

    public static byte[][] fileToChunks(byte[] fileId) throws IOException {
        File file = fileMap.get(fileId).getFile();
        
        FileInputStream fileInput = new FileInputStream(file);

        byte[][] chunks = new byte[Math.toIntExact(file.length() / CHUNK_SIZE + 1)][CHUNK_SIZE];

        for (byte[] chunk : chunks)
            fileInput.read(chunk);

        return chunks;
    }
}
