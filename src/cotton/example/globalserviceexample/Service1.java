package cotton.example.globalserviceexample;
import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import cotton.Cotton;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import cotton.services.DeprecatedService;
import cotton.services.DeprecatedServiceFactory;
import cotton.services.DeprecatedActiveServiceLookup;
import cotton.network.DeprecatedServiceConnection;

/**
 *
 * @author Tony
 **/
public class Service1 implements DeprecatedService{
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        DeprecatedActiveServiceLookup reg = cotton.getServiceRegistation();
        reg.registerService("StringModifier",getFactory(),10);
        cotton.start();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignore) { }
        cotton.shutdown();
        
    }

    public Service1(){
        
    }

    public static DeprecatedServiceFactory getFactory(){
        return new Factory();
    }
    private static class Factory implements DeprecatedServiceFactory{
        @Override
        public DeprecatedService newService(){
            return new Service1();
        }
    }

    public byte[] execute(CloudContext ctx,DeprecatedServiceConnection from, byte[] data,ServiceChain to){
        String s = null;
        System.out.println("Starting ServiceInstance1 ,StringModifier" );
        try{
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            s = (String) input.readObject();
            System.out.println("Before modified: " + s);

        }catch(IOException ex){
            System.out.println("Incomming String: " + s);
        }catch(ClassNotFoundException ex){
            System.out.println("Incomming String:" + s);
        }finally{
            s += " modified";
        }
        return s.getBytes();
    }

}
