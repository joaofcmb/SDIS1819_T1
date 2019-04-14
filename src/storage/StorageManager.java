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

/**
 * Class responsible for managing the backed up files and the stored chunks.
 *
 * The class provides thread-safe methods for performing various actions related to the peer's storage.
 */
public class StorageManager {
    private static final ConcurrentHashMap<String, String> idMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FileInfo> fileMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ChunkInfo> chunkMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> replicationMap = new ConcurrentHashMap<>();

    private static final Object storageLock = new Object();
    private static double usedStorage = 0d;
    private static double maxStorage = Double.MAX_VALUE;

    /**
     * Sets up the filesystem architecture used to manage the peer's storage.
     */
    public static void storageSetup() {
        new File("./peer" + Peer.getId() + "/restored").mkdirs();
        new File("./peer" + Peer.getId() + "/backup").mkdir();
        new File("./peer" + Peer.getId() + "/info").mkdir();
    }

    /**
     * Verifies if a file was already backed up by this peer.
     *
     * @param path Path of the file
     * @return Boolean representing whether the file was backed up or not.
     */
    public static boolean isBackedUp(String path) {
        return idMap.containsKey(new File(path).getAbsolutePath());
    }

    /**
     * Generates an id for a file being backed up.
     *
     * This method also gathers necessary information about the file for later use.
     * @see FileInfo
     *
     * @param path Path of the file
     * @param replicationDegree Desired replication degree for the file
     *
     * @return Id of the file
     *
     * @throws Exception on non existent file or failure to read the file
     */
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

    /**
     * Retrieves the id of a file
     *
     * @param path Path of the file
     *
     * @return Id of the file
     */
    public static String getFileId(String path) {
        return idMap.get(new File(path).getAbsolutePath());
    }

    /**
     * Restores a file on the Peer restored folder
     *
     * @param path Path of the file
     * @param chunks Chunks of the file, retrieved previously
     *
     * @throws IOException on failure to create the restored file
     */
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

    /**
     * Retrives the chunks of a file to be backed up
     *
     * @param fileId Id of the file
     *
     * @return Chunks of the file
     * @throws IOException on failure to read the chunks
     */
    public static byte[][] retrieveFileChunks(String fileId) throws IOException {
        return fileMap.get(fileId).retrieveChunks();
    }

    /**
     * Deletes all information stored about a previously backed up file (Initiator peer)
     *
     * @param path Path of the file
     *
     * @return Id of the file
     */
    public static String deleteFile(String path) {
        File file = new File(path);

        String fileId = idMap.remove(file.getAbsolutePath());
        if (fileId != null)
            fileMap.remove(fileId);

        return fileId;
    }

    /**
     * Stores a chunk, creating all necessary metadata for its management
     * @see ChunkInfo
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     * @param replicationDegree Desired replication degree for the chunk
     * @param body Contents of the chunk
     *
     * @return Boolean representing whether the chunk was already stored or not.
     *
     * @throws IOException on failure to write chunk related files
     */
    public static boolean storeChunk(String fileId, int chunkNo, int replicationDegree, byte[] body) throws IOException {
        double chunkSize = body.length / 1000d;

        synchronized (storageLock) {
            if (StorageManager.usedStorage + chunkSize > StorageManager.maxStorage) return false;

            int replication = getChunkReplication(fileId, chunkNo);
            boolean retVal = chunkMap.putIfAbsent(String.join(":", fileId, String.valueOf(chunkNo)),
                    new ChunkInfo(fileId, chunkNo, replication, replicationDegree, body)) == null;

            if (retVal) {
                StorageManager.usedStorage += chunkSize;
                replicationMap.remove(fileId + chunkNo);
            }

            return retVal;
        }
    }

    /**
     * Deletes all of the stored chunks from a given file
     *
     * @param fileId Id of the file
     */
    public static void deleteChunks(String fileId) {
        synchronized (storageLock) {
            for (String key : chunkMap.keySet()) {
                if (key.split(":")[0].equals(fileId)) {
                    ChunkInfo chunkInfo = chunkMap.remove(key);
                    StorageManager.usedStorage -= chunkInfo.getChunkSize();
                    chunkInfo.delete();
                }
            }

            StorageManager.maxStorage = Double.max(0d, maxStorage);
        }

        new File("./peer" + Peer.getId() + "/backup/" + fileId).delete();
        new File("./peer" + Peer.getId() + "/info/" + fileId).delete();
    }

    /**
     * Updates the storage state after a certain chunk having been stored on a peer
     *
     * This method updates accordingly, regardless of it being the initiator peer or the chunk being stored or not
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @throws IOException on failure to update the data on disk
     */
    public static void signalStoreChunk(String fileId, int chunkNo) throws IOException {
        FileInfo fileInfo = fileMap.get(fileId);
        if (fileInfo != null) {
            fileInfo.incReplication(chunkNo);
            return;
        }

        ChunkInfo chunkInfo = chunkMap.get(String.join(":", fileId, String.valueOf(chunkNo)));
        if (chunkInfo != null) {
            chunkInfo.incReplication();
            return;
        }

        replicationMap.compute(fileId + chunkNo, (key, currVal) -> currVal != null ? currVal++ : 1);
    }

    /**
     * Updates the storage state after a certain chunk having been deleted from a peer
     *
     * This method updates accordingly, regardless of it being the initiator peer or the chunk being stored or not
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @return Information about the deleted chunk
     * @see ChunkInfo
     *
     * @throws IOException on failure to update the data on disk
     */
    public static ChunkInfo signalRemoveChunk(String fileId, int chunkNo) throws IOException {
        FileInfo fileInfo = fileMap.get(fileId);
        if (fileInfo != null) {
            fileInfo.decReplication(chunkNo);
            return null;
        }

        ChunkInfo chunkInfo = chunkMap.get(String.join(":", fileId, String.valueOf(chunkNo)));
        if (chunkInfo != null) {
            chunkInfo.decReplication();
            return chunkInfo;
        }

        replicationMap.compute(fileId + chunkNo, (key, currVal) -> currVal != null ? currVal-- : 0);
        return null;
    }

    /**
     * Retrieves the perceived replication of a chunk, as an initiator peer or as part of the backup enhancement
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @return Perceived replication of the chunk
     */
    public static int getChunkReplication(String fileId, int chunkNo) {
        FileInfo fileInfo = fileMap.get(fileId);
        if (fileInfo != null) {
            return fileInfo.getReplication(chunkNo);
        }

        return replicationMap.computeIfAbsent(fileId + chunkNo, (key) -> 0);
    }

    /**
     * Retrives the number of chunks of a file for the initiator peer
     *
     * @param fileId Id of the file
     *
     * @return Number of chunks for that file
     */
    public static int getChunkNum(String fileId) {
        return fileMap.get(fileId).getChunkNum();
    }

    /**
     * Checks whether this peer has a chunk stored or not
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @return Boolean representing wheather this peer has a chunk stored or not
     */
    public static boolean hasChunk(String fileId, int chunkNo) {
        return chunkMap.containsKey(String.join(":", fileId, String.valueOf(chunkNo)));
    }

    /**
     * Retrieves the contents of a stored chunk
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @return Binary Content of the chunk
     * @throws IOException on failure to read the contents
     */
    public static byte[] getChunk(String fileId, int chunkNo) throws IOException {
        return chunkMap.get(String.join(":", fileId, String.valueOf(chunkNo))).getChunk();
    }

    /**
     * Retrives the current state of the peer storage
     *
     * @return Current state of the peer storage in a human readable manner
     * @throws IOException on failure to retrieve data on disk
     */
    public static synchronized String getState() throws IOException {
        StringBuilder stateInfo =
                new StringBuilder("Peer(" + Peer.getId() + ") - Current State" + System.lineSeparator());

        stateInfo.append("- Backed up Files:")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

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

        stateInfo.append("- Stored Chunks:")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

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

    /**
     * Responsible for updating the maximum storage space and update the storage accordingly.
     *
     * @param diskSpace New maximum storage space
     */
    public static void reclaimSpace(double diskSpace) {
        synchronized (storageLock) {
            StorageManager.maxStorage = Double.max(0d, diskSpace);

            if (StorageManager.usedStorage <= StorageManager.maxStorage) return;

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
