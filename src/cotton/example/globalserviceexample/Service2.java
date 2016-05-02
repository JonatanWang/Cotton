
package cotton.example.globalserviceexample;

import cotton.DeprecatedCotton;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.services.DeprecatedActiveServiceLookup;

public class Service2 {
    public static void main(String[] args) {
        DeprecatedCotton cotton = null;
        try {
            cotton = new DeprecatedCotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }

        DeprecatedActiveServiceLookup lookup = cotton.getServiceRegistation();
        lookup.registerService("TransmissionService", TransmissionService.getFactory(), 10);

        cotton.start();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignore) { }
        cotton.shutdown();
        
    }
}
