package cotton.example.systemintegrationexample;

import cotton.Cotton;
import cotton.network.ClientNetwork;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import java.io.Serializable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.internalRouting.ServiceRequest;
import cotton.internalRouting.InternalRoutingClient;
/**
 *
 * @author magnus
 */
public class ClientPart2 {

    public static void main(String[] args) {
        Cotton cotton = null;
        try {
            cotton = new Cotton(false);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ClientPart2.class.getName()).log(Level.SEVERE, null, ex);
        }
        cotton.start();
        InternalRoutingClient clientNetwork = cotton.getClient();
        ServiceChain chain = new DummyServiceChain()
            .into("StringModifier2").into("StringModifier");
        ServiceRequest serviceRequest = null;
        
        serviceRequest = clientNetwork.sendWithResponse("Cotton cloud is working".getBytes(),chain);
        if(serviceRequest != null) {
            String result = new String(serviceRequest.getData());
            System.out.println("this is the result: " + result);
        }else {
            System.out.println("Failed to send");
        }
        cotton.shutdown();

    }
}
