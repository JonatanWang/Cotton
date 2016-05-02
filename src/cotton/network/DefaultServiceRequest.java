package cotton.network;

import cotton.internalRouting.ServiceRequest;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 * @author Magnus
 **/
public class DefaultServiceRequest implements ServiceRequest{
    private byte[] data= null;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public byte[] getData() {
        boolean loop = false;
        do {
            try {
                latch.await();
                loop = false;
            } catch (InterruptedException ex) {loop = true;}
        }while(loop);
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        latch.countDown();
    }
    
    @Override
    public void setFailed(byte[] errorMessage) {
        this.data = null;
        latch.countDown();
    }
}
