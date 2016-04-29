
package cotton.example.globalserviceexample;

import cotton.Cotton;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.example.FileWriterService;
import cotton.services.DeprecatedActiveServiceLookup;

public class ServiceInstanceFileWriter {
    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }

        DeprecatedActiveServiceLookup lookup = cotton.getServiceRegistation();
        lookup.registerService("TransmissionService", TransmissionService.getFactory(), 10);

        lookup.registerService("FileWriter", FileWriterService.getFactory(), 5);
        cotton.start();
        while(true){
        try {
            Thread.sleep(20000);
        } catch (InterruptedException ignore) { }
        }
        //cotton.shutdown();
        
    }
}
