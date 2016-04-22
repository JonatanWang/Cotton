package cotton.example.globalserviceexample;
import cotton.services.ServiceInstance;
import cotton.services.ServiceFactory;
import cotton.services.CloudContext;
import cotton.network.ServiceConnection;
import cotton.network.ServiceChain;
import java.io.Serializable;
import cotton.Cotton;
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
 **/
public class ServiceInstance1 implements ServiceInstance{
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        ActiveServiceLookup reg = cotton.getServiceRegistation();
        reg.registerService("StringModifier",getFactory(),10);
        cotton.start();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignore) { }
        cotton.shutdown();
        
    }

    public ServiceInstance1(){
        
    }

    public static ServiceFactory getFactory(){
        return new Factory();
    }
    private static class Factory implements ServiceFactory{
        @Override
        public ServiceInstance newServiceInstance(){
            return new ServiceInstance1();
        }
    }

    public Serializable consumeServiceOrder(CloudContext ctx,ServiceConnection from, InputStream data,ServiceChain to){
        String s = null;
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
