
package cotton.example.scalingexample;

import cotton.Cotton;
import cotton.servicediscovery.CircuitBreakerLogger;
import cotton.services.ActiveServiceLookup;
import cotton.test.services.GlobalDiscoveryAddress;

import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author magnus
 */
public class CircuitBreakerLoggerExample {
    public static void main(String[] args) throws UnknownHostException {
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress("127.0.0.1", 5888);
        //GlobalDnsStub gDns = getDnsStub(null, 9546);
        Cotton cotton = new Cotton(false, gDns);
        ActiveServiceLookup serviceRegistation = cotton.getServiceRegistation();
        serviceRegistation.registerService("CircuitBreakerLogger", CircuitBreakerLogger.getFactory(), 2);
        System.out.println("Starting...");
        cotton.start();
        Scanner scan = new Scanner(System.in);
        boolean run = true;
        while(run) {
            try {
                if(Integer.parseInt(scan.nextLine()) == 1)
                    run = false;
            } catch(Exception e) {}
        }
        cotton.shutdown();
    }
}
//GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress(port);