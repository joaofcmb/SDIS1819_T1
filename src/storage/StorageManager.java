package storage;

import peer.Peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static String generateFileId(String path) throws NoSuchAlgorithmException {
        File file = new File(path);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest((file.getAbsolutePath() + file.length() + file.lastModified()).getBytes());

        String fileId = String.format("%064x", new BigInteger(1, digest));

        idMap.put(file.getAbsolutePath(), fileId);
        fileMap.put(fileId, new FileInfo(file));

        return fileId;
    }

    public static String getFileId(String path) {
        return idMap.get(new File(path).getAbsolutePath());
    }


    public static synchronized void restoreFile(String path, byte[][] chunks) throws IOException {
        File file = new File("./peer" + Peer.getId() + "/restored/" + new File(path).getName());

        try (FileOutputStream fos = new FileOutputStream(file)) {
            for (byte[] chunk : chunks) {
                fos.write(chunk);
            }
        }
    }

    public static byte[][] retrieveFileChunks(String fileId) throws IOException {
        return fileMap.get(fileId).retrieveChunks();
    }

    public static void storeChunk(String fileId, int chunkNo, int replicationDegree, byte[] body) throws IOException {
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

    public static int getChunkNum(String fileId) {
        return fileMap.get(fileId).getChunkNum();
    }

    public static boolean hasChunk(String fileId, int chunkNo) {
        return chunkMap.containsKey(fileId + chunkNo);
    }

    public static byte[] getChunk(String fileId, int chunkNo) throws IOException {
        return chunkMap.get(fileId + chunkNo).getChunk();
    }
}
