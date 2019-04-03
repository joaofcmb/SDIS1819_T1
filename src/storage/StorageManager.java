package storage;

import peer.Peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

public class StorageManager {
    private static final ConcurrentHashMap<String, String> idMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, FileInfo> fileMap = new ConcurrentHashMap<>();

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

        String fileId = Base64.getEncoder().encodeToString(sha256.digest((file.getAbsolutePath() + file.length() + file.lastModified()).getBytes()));

        idMap.put(file.getAbsolutePath(), fileId);
        fileMap.put(fileId, new FileInfo(file));

        return fileId;
    }

    public static byte[][] fileToChunks(String fileId) {
        return fileMap.get(fileId).getChunks();
    }

    public static synchronized void storeChunk(String fileId, int chunkNo, int replicationDegree, String body) throws IOException {

        File chunkFile = new File("./peer" + Peer.getId() + "/backup/" + fileId + "/chk" + chunkNo);

        System.out.println(chunkFile.getAbsolutePath());

        if (!chunkFile.exists()) {
            chunkFile.getParentFile().mkdirs();
            chunkFile.createNewFile();

            FileOutputStream out = new FileOutputStream(chunkFile);
            out.write(body.getBytes());
            out.close();

            File infoFile = new File("./peer" + Peer.getId() + "/info/" + fileId + "/chk" + chunkNo);
            infoFile.getParentFile().mkdirs();
            infoFile.createNewFile();

            PrintWriter printer = new PrintWriter(infoFile);
            printer.println(replicationDegree - 1);
            printer.close();
        }
    }

    public static synchronized void signalStoreChunk(String fileId, int chunkNo) {
        if (fileMap.containsKey(fileId)) {
            fileMap.get(fileId).incReplication(chunkNo);
        }
        else {
            try {
                File infoFile = new File("./peer" + Peer.getId() + "/info/" + fileId + "/chk" + chunkNo);

                if (infoFile.exists()) {
                    Scanner in = new Scanner(infoFile);
                    PrintWriter printer = new PrintWriter(infoFile);

                    printer.println(in.nextInt() - 1);
                    in.close();
                    printer.close();
                }

            } catch (IOException e) {
                System.err.println("WARNING: Failed to write to backupInfo.txt");
            }
        }
    }

    public static int getChunkReplication(String fileId, int chunkNo) {
            return fileMap.containsKey(fileId) ? fileMap.get(fileId).getReplication(chunkNo) : -1;
    }
}
