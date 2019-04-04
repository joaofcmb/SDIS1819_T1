package storage;

import peer.Peer;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private static final ConcurrentHashMap<String, String> idMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FileInfo> fileMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChunkInfo> chunkMap = new ConcurrentHashMap<>();

    public static void storageSetup() {
        // TODO try getting rid of this
        new File("./peer" + Peer.getId() + "/restored").mkdirs();
        new File("./peer" + Peer.getId() + "/backup").mkdir();
        new File("./peer" + Peer.getId() + "/info").mkdir();
    }

    public static boolean isBackedUp(String path) {
        return idMap.containsKey(new File(path).getAbsolutePath());
    }

    public static String fileId(String path) throws NoSuchAlgorithmException, IOException {
        File file = new File(path);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest((file.getAbsolutePath() + file.length() + file.lastModified()).getBytes());

        String fileId = String.format("%064x", new BigInteger(1, digest));

        idMap.put(file.getAbsolutePath(), fileId);
        fileMap.put(fileId, new FileInfo(file));

        return fileId;
    }

    public static byte[][] fileToChunks(String fileId) {
        return fileMap.get(fileId).getChunks();
    }

    public static void storeChunk(String fileId, int chunkNo, int replicationDegree, String body) throws IOException {

        if (!chunkMap.containsKey(fileId + chunkNo)) {
            chunkMap.put(fileId + chunkNo, new ChunkInfo(fileId, chunkNo, replicationDegree, body));
        }
    }

    public static void signalStoreChunk(String fileId, int chunkNo) throws IOException {
        if (fileMap.containsKey(fileId)) {
            fileMap.get(fileId).incReplication(chunkNo);
        }
        else if (chunkMap.containsKey(fileId + chunkNo)) {
            chunkMap.get(fileId + chunkNo).incReplication();
        }
    }

    public static int getChunkReplication(String fileId, int chunkNo) {
        return fileMap.containsKey(fileId) ? fileMap.get(fileId).getReplication(chunkNo) : -1;
    }
}
