package storage;

import peer.Peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private static final ConcurrentHashMap<String, String> idMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FileInfo> fileMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChunkInfo> chunkMap = new ConcurrentHashMap<>();

    private static final Object storageLock = new Object();
    private static double usedStorage = 0d;
    private static double maxStorage = Double.MAX_VALUE;

    public static void storageSetup() {
        new File("./peer" + Peer.getId() + "/restored").mkdirs();
        new File("./peer" + Peer.getId() + "/backup").mkdir();
        new File("./peer" + Peer.getId() + "/info").mkdir();
    }

    public static boolean isBackedUp(String path) {
        return idMap.containsKey(new File(path).getAbsolutePath());
    }

    public static String generateFileId(String path, int replicationDegree) throws Exception {
        File file = new File(path);
        if (!file.exists()) throw new FileNotFoundException();

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] digest = sha256.digest(
                (file.getAbsolutePath() + file.length() + file.lastModified() + Peer.getId()).getBytes()
        );

        String fileId = String.format("%064x", new BigInteger(1, digest));

        idMap.put(file.getAbsolutePath(), fileId);
        fileMap.put(fileId, new FileInfo(file, replicationDegree));

        return fileId;
    }

    public static String getFileId(String path) {
        return idMap.get(new File(path).getAbsolutePath());
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

    public static String deleteFile(String path) {
        File file = new File(path);

        String fileId = idMap.remove(file.getAbsolutePath());
        if (fileId != null)
            fileMap.remove(fileId);

        return fileId;
    }

    public static boolean storeChunk(String fileId, int chunkNo, int replicationDegree, byte[] body) throws IOException {
        double chunkSize = body.length / 1000d;

        synchronized (storageLock) {
            if (StorageManager.usedStorage + chunkSize > StorageManager.maxStorage) return false;

            boolean retVal = chunkMap.putIfAbsent(String.join(":", fileId, String.valueOf(chunkNo)),
                    new ChunkInfo(fileId, chunkNo, replicationDegree, body)) == null;

            if (retVal) StorageManager.usedStorage += chunkSize;

            return retVal;
        }
    }

    public static void deleteChunks(String fileId) {
        synchronized (storageLock) {
            synchronized (chunkMap) {
                for (String key : chunkMap.keySet()) {
                    if (key.split(":")[0].equals(fileId)) {
                        ChunkInfo chunkInfo = chunkMap.remove(key);
                        StorageManager.usedStorage -= chunkInfo.getChunkSize();
                        chunkInfo.delete();
                    }
                }
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
    }

    public static ChunkInfo signalRemoveChunk(String fileId, int chunkNo) throws IOException {
        synchronized (fileMap) {
            if (fileMap.containsKey(fileId)) {
                fileMap.get(fileId).decReplication(chunkNo);
                return null;
            }
        }
        synchronized (chunkMap) {
            String key = String.join(":", fileId, String.valueOf(chunkNo));
            if (chunkMap.containsKey(key)) {
                ChunkInfo chunkInfo = chunkMap.get(key);
                chunkInfo.decReplication();
                return chunkInfo;
            }
        }

        return null;
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
                            .append(": Perceived Replication ")
                            .append(fileInfo.getReplication(i))
                            .append(System.lineSeparator());
                }

                stateInfo.append(System.lineSeparator());
            }
        }

        stateInfo.append("- Stored Chunks:")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        synchronized (chunkMap) {
            for (String key : chunkMap.keySet()) {
                ChunkInfo chunkInfo = chunkMap.get(key);

                stateInfo.append("  - ID (\"fileId\":\"chunkNo\"): ")
                        .append(key)
                        .append(System.lineSeparator())
                        .append("  - Chunk Size: ")
                        .append(chunkInfo.getChunkSize())
                        .append(" KBytes")
                        .append(System.lineSeparator())
                        .append("  - Perceived replication: ")
                        .append(chunkInfo.getReplication())
                        .append(System.lineSeparator())
                        .append(System.lineSeparator());
            }
        }

        stateInfo.append("- Maximum Storage: ")
                .append(StorageManager.maxStorage < Double.MAX_VALUE ?
                        StorageManager.maxStorage + " KBytes"
                        : "unlimited")
                .append(System.lineSeparator())
                .append("- Used Storage: ")
                .append(StorageManager.usedStorage)
                .append(" KBytes")
                .append(System.lineSeparator());

        return stateInfo.toString();
    }

    public static void reclaimSpace(double diskSpace) {
        synchronized (storageLock) {
            StorageManager.maxStorage = Double.max(0f, diskSpace);

            if (StorageManager.usedStorage <= StorageManager.maxStorage) return;

            synchronized (chunkMap) {
                PriorityQueue<String> chunkQueue = new PriorityQueue<>(chunkMap.size(),
                        Comparator.comparing(chunkMap::get));
                chunkQueue.addAll(chunkMap.keySet());

                while (StorageManager.usedStorage > StorageManager.maxStorage) {
                    String key = Objects.requireNonNull(chunkQueue.poll());
                    String[] keySplit = key.split(":");

                    ChunkInfo chunkInfo = chunkMap.remove(key);
                    StorageManager.usedStorage -= chunkInfo.getChunkSize();
                    chunkInfo.delete();

                    Peer.mc.sendMessage(new String[] {"REMOVED", Peer.getProtocolVersion(), Peer.getId(),
                            keySplit[0], keySplit[1]} );
                }
            }
        }
    }
}
