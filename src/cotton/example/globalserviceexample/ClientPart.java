package cotton.example.globalserviceexample;

import cotton.DeprecatedCotton;
import cotton.network.ClientNetwork;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import java.io.Serializable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.network.DeprecatedServiceRequest;

/**
 *
 * @author magnus
 */
public class ClientPart {

    public static void main(String[] args) {
        DeprecatedCotton cotton = null;
        try {
            cotton = new DeprecatedCotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(GlobalDiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        cotton.start();
        ClientNetwork clientNetwork = cotton.getClientNetwork();
        ServiceChain chain = new DummyServiceChain()
            .into("TransmissionService").into("StringModifier");
        DeprecatedServiceRequest serviceRequest = null;
        try {
            serviceRequest = clientNetwork.sendToService("Hej".getBytes(), chain);
        }
        catch (IOException e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        if(serviceRequest != null) {
            String result = new String(serviceRequest.getData());
            System.out.println("this is the result: " + result);
        }else {
            System.out.println("Failed to send");
        }

        cotton.shutdown();

    }
}
