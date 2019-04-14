package multicast;

import peer.Peer;

/**
 * Thread associtated with a multicast channel, responsible for receiving its messages and dispatching them to a worker
 *
 * @see MulticastWorker
 */
public class MulticastThread extends Thread {
    private MulticastInterface multicastInterface;

    /**
     * Constructor creating a thread to dispatch messages to workers
     *
     * @param multicastInterface Interface of the multicast channel this thread is responsible for
     */
    public MulticastThread(MulticastInterface multicastInterface) {
        this.multicastInterface = multicastInterface;
    }

    /**
     * Implements the Thread behavior, endlessly receiving messages and dispatching them to a worker
     */
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            Peer.getMulticastThreadPool().execute(new MulticastWorker(multicastInterface.receiveMessage()));
        }
    }
}
