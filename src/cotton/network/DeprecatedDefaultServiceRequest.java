
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
public class DeprecatedDefaultServiceRequest implements DeprecatedServiceRequest{
    private byte[] data = null;
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
    public void setFailed() {
        this.data = null;
        latch.countDown();
    }
}    

