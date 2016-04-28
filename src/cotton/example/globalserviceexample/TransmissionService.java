package cotton.example.globalserviceexample;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.Service;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TransmissionService implements Service {
    @Override
    public byte[] execute(CloudContext ctx, ServiceConnection from, byte[] data, ServiceChain to) {

        String text = null;
        System.out.println("Starting ServiceInstancew ,TransmissionService" );

        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream input = new ObjectInputStream(in);
            text = (String)input.readObject();
            System.out.println("In try: " +text);
        } catch (IOException ex) {
            Logger.getLogger(TransmissionService.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(TransmissionService.class.getName()).log(Level.SEVERE, null, ex);
        }finally {

            text = text + ". Cotton clouds in the sky!";
            System.out.println("In finally: " +text);
        }

        return text.getBytes();
    }

    public static ServiceFactory getFactory(){
        return new Factory();
    }

    public static class Factory implements ServiceFactory {
        @Override
        public Service newService() {
            return new TransmissionService();
        }
    }
}
