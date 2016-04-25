package cotton.example.globalserviceexample;
import cotton.services.ServiceInstance;
import cotton.services.ServiceFactory;
import cotton.services.CloudContext;
import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.Serializable;
import cotton.Cotton;
import cotton.example.ImageManipulationService;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.ObjectInputStream;
import cotton.services.ActiveServiceLookup;
import java.io.IOException;

/**
 *
 * @author Tony
 * @author Gunnlaugur
 **/
public class ServiceInstanceImageManipulation implements ServiceInstance{
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false, 4444);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        ActiveServiceLookup reg = cotton.getServiceRegistation();
        reg.registerService("StringModifier",getFactory(),10);
        reg.registerService("ImageManipulation", ImageManipulationService.getFactory(), 1);
        
        cotton.start();
        while(true) {
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ignore) { }
        }
        
        //cotton.shutdown();
        
    }

    public ServiceInstanceImageManipulation(){
        
    }

    public static ServiceFactory getFactory(){
        return new Factory();
    }
    private static class Factory implements ServiceFactory{
        @Override
        public ServiceInstance newServiceInstance(){
            return new ServiceInstanceImageManipulation();
        }
    }

    public Serializable consumeServiceOrder(CloudContext ctx,ServiceConnection from, InputStream data,ServiceChain to){
        String s = null;
        System.out.println("Starting ServiceInstance1 ,StringModifier" );
        try{
            ObjectInputStream input = new ObjectInputStream(data);
            s = (String) input.readObject();
            System.out.println("Before modified: " + s);

        }catch(IOException ex){
            System.out.println("Incomming String: " + s);
        }catch(ClassNotFoundException ex){
            System.out.println("Incomming String:" + s);
        }finally{
            s += " modified";
        }
        return s;
    }

}
