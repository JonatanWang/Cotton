
package cotton.example.globalserviceexample;

import cotton.DeprecatedCotton;
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
        DeprecatedCotton cotton = null;
        try {
            cotton = new DeprecatedCotton(true);
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
