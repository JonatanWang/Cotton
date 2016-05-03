package cotton.example.systemintegrationexample;
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
import cotton.services.Service;
import cotton.services.ServiceFactory;
import cotton.services.ActiveServiceLookup;
import cotton.network.Origin;
/**
 *
 * @author Tony
 * @author Magnus
 **/
public class StringModifier2 implements Service{
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(StringModifier2.class.getName()).log(Level.SEVERE, null, ex);
        }
        ActiveServiceLookup reg = cotton.getServiceRegistation();
        reg.registerService("StringModifier2",getFactory(),10);
        cotton.start();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignore) { }
        cotton.shutdown();
        
    }

    public StringModifier2(){
        
    }

    public static ServiceFactory getFactory(){
        return new Factory();
    }
    private static class Factory implements ServiceFactory{
        @Override
        public Service newService(){
            return new StringModifier2();
        }
    }

    public byte[] execute(CloudContext ctx,Origin origin, byte[] data,ServiceChain to){
        String s = new String(data);
        System.out.println("before:" + s);
        s += "Cotton Cloud";
        System.out.println("After:" + s);
        return s.getBytes();
    }

}
