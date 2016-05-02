package cotton.network;

import java.util.concurrent.CountDownLatch;

/**
 * The <code>SocketLatch</code> is designed to block the program until an incoming 
 * packet is received or set as failed. This latch is designed to work with the 
 * keep alive message.
 * 
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 * @author Magnus
 **/
public class SocketLatch{
    private NetworkPacket networkPacket = null;
    private CountDownLatch latch = new CountDownLatch(1);

    /**
     * Blocks the program until the <code>NetworkPacket</code> is set as received 
     * or failed.
     * 
     * @return returns the set <code>NetworkPacket</code>.
     */
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

    /**
     * Sets the <code>NetworkPacket</code> as well as executes the latch countdown.
     * 
     * @param networkPacket the <code>NetworkPacket</code> releasing the latch.
     */
    public void setData(NetworkPacket networkPacket) {
        this.networkPacket = networkPacket;
        latch.countDown();
    }
    
    /**
     * Sets the <code>NetworkPacket</code> as an packet containing the error and 
     * releases the latch.
     * 
     * @param errorMessage the <code>NetworkPacket</code> containing the error.
     */
    public void setFailed(NetworkPacket errorMessage) {
        networkPacket = errorMessage;
        latch.countDown();
    }
}
