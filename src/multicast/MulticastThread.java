package multicast;

public class MulticastThread extends Thread {
    private MulticastInterface multicastInterface;

    public MulticastThread(MulticastInterface multicastInterface) {
        this.multicastInterface = multicastInterface;
    }

    @Override
    public void run() {

    }
}
