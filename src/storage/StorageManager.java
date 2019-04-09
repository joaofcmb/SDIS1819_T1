package storage;

import peer.Peer;

import java.io.File;
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
        new File("./peer" + Peer.getId() + "/restored").mkdirs();
        new File("./peer" + Peer.getId() + "/backup").mkdir();
        new File("./peer" + Peer.getId() + "/info").mkdir();
    }

    public static boolean isBackedUp(String path) {
        return idMap.containsKey(new File(path).getAbsolutePath());
    }

    public static String generateFileId(String path, int replicationDegree) throws NoSuchAlgorithmException {
        File file = new File(path);
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest((file.getAbsolutePath() + file.length() + file.lastModified()).getBytes());

        String fileId = String.format("%064x", new BigInteger(1, digest));

        idMap.put(file.getAbsolutePath(), fileId);
        fileMap.put(fileId, new FileInfo(file, replicationDegree));

        return fileId;
    }

    public static String getFileId(String path) {
        return idMap.get(path);
    }

    public static synchronized void restoreFile(String path, byte[][] chunks) throws IOException {
        File file = new File("./peer" + Peer.getId() + "/restored/" + new File(path).getName());
        if (file.exists())  file.delete();
        file.createNewFile();

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
        String key = String.join(":", fileId, String.valueOf(chunkNo));
        synchronized (chunkMap) {
            if (chunkMap.containsKey(key))
                chunkMap.get(key).resetReplication(replicationDegree);
            else
                chunkMap.put(key, new ChunkInfo(fileId, chunkNo, replicationDegree, body));
        }
    }

    public static String deleteFile(String path) {
        File file = new File(path);

        String fileId = idMap.remove(file.getAbsolutePath());
        if (fileId != null)
            fileMap.remove(fileId);

        return fileId;
    }

    public static void deleteChunks(String fileId) {
        synchronized (chunkMap) {
            for (String key : chunkMap.keySet()) {
                if (key.split(":")[0].equals(fileId))
                chunkMap.remove(key).delete();
            }
        }

        new File("./peer" + Peer.getId() + "/backup/" + fileId).delete();
        new File("./peer" + Peer.getId() + "/info/" + fileId).delete();
    }

    public static void signalStoreChunk(String fileId, int chunkNo) throws IOException {
        synchronized (fileMap) {
            if (fileMap.containsKey(fileId)) {
                fileMap.get(fileId).incReplication(chunkNo);
                return;
            }
        }
        synchronized (chunkMap) {
            String key = String.join(":", fileId, String.valueOf(chunkNo));
            if (chunkMap.containsKey(key)) {
                chunkMap.get(key).incReplication();
            }
        }
        return;
    }

    public static void resetChunkReplication(String fileId, int chunkNo) {
        fileMap.get(fileId).resetReplication(chunkNo);
    }

    public static int getChunkReplication(String fileId, int chunkNo) {
        synchronized (fileMap) {
            return fileMap.containsKey(fileId) ? fileMap.get(fileId).getReplication(chunkNo) : -1;
        }
    }

    public static int getChunkNum(String fileId) {
        return fileMap.get(fileId).getChunkNum();
    }

    public static boolean hasChunk(String fileId, int chunkNo) {
        return chunkMap.containsKey(String.join(":", fileId, String.valueOf(chunkNo)));
    }

    public static byte[] getChunk(String fileId, int chunkNo) throws IOException {
        return chunkMap.get(String.join(":", fileId, String.valueOf(chunkNo))).getChunk();
    }

    public static synchronized String getState() throws IOException {
        StringBuilder stateInfo =
                new StringBuilder("Peer(" + Peer.getId() + ") - Current State" + System.lineSeparator());

        stateInfo.append("- Backed up Files:")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        synchronized (idMap) {
            for (String path : idMap.keySet()) {
                String fileId = idMap.get(path);
                FileInfo fileInfo = fileMap.get(fileId);

                stateInfo.append("  - File Path: ")
                        .append(path)
                        .append(System.lineSeparator())
                        .append("  - File Id: ")
                        .append(fileId)
                        .append(System.lineSeparator())
                        .append("  - Desired Replication Degree: ")
                        .append(fileInfo.getReplicationDegree())
                        .append(System.lineSeparator());

                for (int i = 0; i < fileInfo.getChunkNum(); i++) {
                    stateInfo.append("    - Chunk ")
                            .append(i)
                            .append(": Replication ")
                            .append(fileInfo.getReplication(i))
                            .append(System.lineSeparator());
                }

                stateInfo.append(System.lineSeparator());
            }
        }

        stateInfo.append("- Stored Chunks:")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        float usedStorage = 0f;
        synchronized (chunkMap) {
            for (String key : chunkMap.keySet()) {
                ChunkInfo chunkInfo = chunkMap.get(key);
                float chunkSize = chunkInfo.getChunkSize();
                usedStorage += chunkSize;

                stateInfo.append("  - ID (\"fileId\":\"chunkNo\"): ")
                        .append(key)
                        .append(System.lineSeparator())
                        .append("  - Chunk Size: ")
                        .append(chunkSize)
                        .append(" KBytes")
                        .append(System.lineSeparator())
                        .append("  - Redundancy (Difference of perceived and desired replication): ")
                        .append(chunkInfo.getRedundancy())
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
            }
        }

        stateInfo.append("- Maximum Storage: ")
                .append("Unlimited") // TODO Print value of max storage if set
                .append(" KBytes")
                .append(System.lineSeparator())
                .append("- Used Storage: ")
                .append(usedStorage)
                .append( "KBytes")
                .append(System.lineSeparator());

        return stateInfo.toString();
    }
}
