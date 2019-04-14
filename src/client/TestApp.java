package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Class implementing a client for running the remote operations on a peer.
 */
public class TestApp {
    /**
     * Main method used to invoke the remote operations based on the command line arguments
     *
     * @param args command line arguments used for specifying the operation to be executed and its arguments
     */
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry();
            ClientInterface stub = (ClientInterface) registry.lookup(args[0]);

            switch (args[1]) {
                case "BACKUP":
                    if (stub.backup(args[2], Integer.parseInt(args[3])))
                        System.out.println("Backup command for \"" + args[2] + "\" was successful.");
                    else
                        System.out.println("WARNING: Backup command ended. Replication degree not fully met");
                    break;
                case "RESTORE":
                    if (stub.restore(args[2]))
                        System.out.println("Restore command for \"" + args[2] + "\" was successful.");
                    else
                        System.out.println("ERROR: Restore command has failed.");
                    break;
                case "DELETE":
                    if (stub.delete(args[2]))
                        System.out.println("Delete command for \"" + args[2] + "\" was successful.");
                    else
                        System.out.println("ERROR: Delete command has failed.");
                    break;
                case "RECLAIM":
                    if (stub.reclaim(Double.parseDouble(args[2])))
                        System.out.println("Reclaim command was successful (Max Storage: " + args[2] + ").");
                    else
                        System.out.println("ERROR: Reclaim command has failed.");
                    break;
                case "STATE":
                    String stateInfo = stub.state();
                    if (stateInfo != null)
                        System.out.print(stateInfo);
                    else
                        System.out.println("ERROR: State command has failed.");
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("TestApp - RMI exception: " + e.toString());
        }
    }
}