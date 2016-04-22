package cotton.example.globalserviceexample;

import cotton.Cotton;
import cotton.network.ClientNetwork;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import cotton.network.ServiceRequest;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author magnus
 */
public class ClientPart {

    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        cotton.start();
        ClientNetwork clientNetwork = cotton.getClientNetwork();
        ServiceChain chain = new DummyServiceChain()
                .into("TransmissionService").into("StringModifier");
        ServiceRequest serviceRequest = clientNetwork.sendToService("Hej", chain);
        if(serviceRequest != null) {
            String result = (String) serviceRequest.getData();
            System.out.println("this is the result: " + result);
        }else {
            System.out.println("Failed to send");
        }
        
        cotton.shutdown();

    }
}
