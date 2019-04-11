package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        //TODO Verify arguments
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
                    stub.reclaim(Double.parseDouble(args[2]));
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