
package cotton.example.globalserviceexample;

import cotton.Cotton;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magnus
 */
public class GlobalDiscoveryExample {

    public GlobalDiscoveryExample() {
        
    }
    
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(true);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        cotton.start();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ignore) { }
        cotton.shutdown();
        
    }
    
}
