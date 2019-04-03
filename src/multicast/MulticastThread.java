package multicast;

import peer.Peer;

public class MulticastThread extends Thread {
    private MulticastInterface multicastInterface;

    public MulticastThread(MulticastInterface multicastInterface) {
        this.multicastInterface = multicastInterface;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Peer.getMulticastThreadPool().execute(new MulticastWorker(multicastInterface.receiveMessage()));
        }
    }
}
