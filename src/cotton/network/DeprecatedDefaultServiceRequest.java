
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
    private Serializable data = null;
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
        return data;
    }

    public void setData(Serializable data) {
        this.data = data;
        latch.countDown();
    }
    public void setFailed() {
        this.data = null;
        latch.countDown();
    }
}    

