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
                        System.out.println("Backup protocol for \"" + args[2] + "\" successful.");
                    else
                        System.out.println("WARNING: Backup protocol ended. Replication degree not fully met");
                    break;
                case "RESTORE":
                    if (stub.restore(args[2]))
                        System.out.println("Restore protocol for \"" + args[2] + "\" successful.");
                    else
                        System.out.println("ERROR: Restore protocol failed.");
                    break;
                case "DELETE":
                    stub.delete(args[2]);
                    break;
                case "RECLAIM":
                    stub.reclaim(Integer.parseInt(args[2]));
                    break;
                case "STATE":
                    stub.state();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("TestApp - RMI exception: " + e.toString());
            e.printStackTrace();
        }
    }
}