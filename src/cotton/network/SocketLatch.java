package cotton.network;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;


/**
 *
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 * @author Magnus
 **/
public class SocketLatch implements ServiceRequest{
    private NetworkPacket networkPacket = null;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public Serializable getData() {
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
    public void setFailed() {
        this.networkPacket = null;
        latch.countDown();
    }
}
