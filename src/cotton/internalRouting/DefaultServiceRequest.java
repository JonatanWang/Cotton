/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.internalRouting;

import cotton.internalRouting.ServiceRequest;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author tony
 */
public class DefaultServiceRequest implements ServiceRequest{
    private byte[] data = null;
    private CountDownLatch latch = new CountDownLatch(1);

    
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
    public void setFailed(byte[] errorMessage) {
        data = errorMessage;
        latch.countDown();
    }
}
