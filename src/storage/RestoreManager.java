package storage;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RestoreManager {
    private static final ConcurrentHashMap<String, Object> flagMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Optional<byte[]>> chunkMap = new ConcurrentHashMap<>();

    public static void markChunk(String fileId, int chunkNo) {
        flagMap.putIfAbsent(fileId + chunkNo, new Object());
    }

    public static boolean checkAndUnMarkChunk(String fileId, int chunkNo) {
        return flagMap.remove(fileId + chunkNo) != null;
    }

    public static void unMarkChunk(String fileId, int chunkNo) {
        flagMap.remove(fileId + chunkNo);
    }


    public static void addChunk(String fileId, int chunkNo) {
        chunkMap.put(fileId + chunkNo, Optional.empty());
    }

    public static void putChunk(String fileId, int chunkNo, byte[] body) {
        chunkMap.replace(fileId + chunkNo, Optional.of(body));
    }

    public static byte[] retrieveChunk(String fileId, int chunkNo) {
        return chunkMap.remove(fileId + chunkNo).orElse(null);
    }
}
