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
public class GlobalDiscoveryExample2{
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(true,4444);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample2.class.getName()).log(Level.SEVERE, null, ex);
        }
        cotton.start();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ignore) { }
        cotton.shutdown();
        
    }
}
