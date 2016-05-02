package cotton.network;

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 * @author Magnus
 **/
public class SocketLatch{
    private NetworkPacket networkPacket = null;
    private CountDownLatch latch = new CountDownLatch(1);

    public NetworkPacket getData() {
        boolean loop = false;
        do {
            try {
                latch.await();
                loop = false;
            } catch (InterruptedException ex) {loop = true;}
        }while(loop);
        return networkPacket;
    }

    public void setData(NetworkPacket networkPacket) {
        this.networkPacket = networkPacket;
        latch.countDown();
    }
    public void setFailed(NetworkPacket errorMessage) {
        networkPacket = errorMessage;
        latch.countDown();
    }
}
