package storage;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class responsible for managing the restore of chunks
 *
 * This class keeps track of the restored chunks (Initiator peer) and of whether a chunk restore has already
 * been processed or not (Peer with a stored chunk)
 *
 * This is mostly necessary to communicate between the different threads
 */
public class RestoreManager {
    private static final ConcurrentHashMap<String, Object> flagMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Optional<byte[]>> chunkMap = new ConcurrentHashMap<>();

    /**
     * Marks a chunk to be restored
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     */
    public static void markChunk(String fileId, int chunkNo) {
        flagMap.putIfAbsent(fileId + chunkNo, new Object());
    }

    /**
     * Checks if a chunk is marked to be restored and unmarks it
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @return Boolean representing wheather a chunk was marked or not
     */
    public static boolean checkAndUnMarkChunk(String fileId, int chunkNo) {
        return flagMap.remove(fileId + chunkNo) != null;
    }

    /**
     * Unmarks a chunk
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     */
    public static void unMarkChunk(String fileId, int chunkNo) {
        flagMap.remove(fileId + chunkNo);
    }


    /**
     * Adds a chunk request for restore (Initiator Peer)
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     */
    public static void addChunk(String fileId, int chunkNo) {
        chunkMap.put(fileId + chunkNo, Optional.empty());
    }

    /**
     * Stores the contents of a restored chunk if there's a request for it (Initiator Peer)
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     * @param body Binary content of the chunk
     *
     */
    public static void putChunk(String fileId, int chunkNo, byte[] body) {
        try {
            chunkMap.replace(fileId + chunkNo, Optional.of(body));
        } catch( NullPointerException ignored) {
        }
    }

    /**
     * Retrieves the contents of a chunk request (Initiator Peer)
     *
     * @param fileId Id of the file
     * @param chunkNo Id of the chunk
     *
     * @return Binary content of the chunk. If there are no contents, null is returned.
     */
    public static byte[] retrieveChunk(String fileId, int chunkNo) {
        return chunkMap.remove(fileId + chunkNo).orElse(null);
    }
}
