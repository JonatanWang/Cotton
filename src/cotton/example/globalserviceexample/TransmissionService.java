package cotton.example.globalserviceexample;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TransmissionService implements ServiceInstance {
    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {

        String text = null;
        System.out.println("Starting ServiceInstancew ,TransmissionService" );

        try {
            ObjectInputStream input = new ObjectInputStream(data);
            text =  (String)input.readObject();
            System.out.println("In try: " +text);
        } catch (IOException ex) {
            Logger.getLogger(TransmissionService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TransmissionService.class.getName()).log(Level.SEVERE, null, ex);
        }finally {

            text.concat(". Cotton clouds in the sky!");
            System.out.println("In finally: " +text);
        }

        return text;
    }
    
    public static ServiceFactory getFactory(){
        return new Factory();
    }
    public static class Factory implements ServiceFactory {
        @Override
        public ServiceInstance newServiceInstance() {
            return new TransmissionService();
        }
    }
}
