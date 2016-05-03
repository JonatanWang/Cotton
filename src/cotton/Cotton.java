package cotton;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import cotton.example.FileWriterService;
import cotton.example.ImageManipulationPacket;
import cotton.example.ImageManipulationService;
import cotton.network.ClientNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.ServiceChain;
import cotton.servicediscovery.DefaultLocalServiceDiscovery;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.services.DefaultActiveServiceLookup;
import cotton.network.DummyServiceChain;
import cotton.servicediscovery.DefaultGlobalServiceDiscovery;
import java.util.concurrent.ThreadLocalRandom;
import cotton.network.NetworkHandler;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceHandler;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.services.ServiceLookup;
import cotton.services.ServiceFactory;
import cotton.internalRouting.DefaultInternalRouting;
import cotton.internalRouting.InternalRoutingClient;
import java.util.Random;
/**
 *
 * @author Jonathan
 * @author Magnus
 * @author Gunnlaugur
 * @author Tony
 */
public class Cotton {
    private ActiveServiceLookup lookup;
    private NetworkHandler network;
    private ServiceHandler services;
    private ServiceDiscovery discovery;
    private DefaultInternalRouting internalRouting;

    public Cotton (boolean globalServiceDiscovery) throws java.net.UnknownHostException {
        Random rnd = new Random();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        NetworkHandler net = null;
        if(globalServiceDiscovery) {
            net = new DefaultNetworkHandler(rnd.nextInt(20000)+3000);
            discovery = new GlobalServiceDiscovery(globalDiscoveryDNS);
        
        }else {
            net = new DefaultNetworkHandler(rnd.nextInt(20000)+3000);
            discovery = new LocalServiceDiscovery(globalDiscoveryDNS);
        
        }
        lookup = new ServiceLookup();
        discovery.setLocalServiceTable(lookup);
        this.internalRouting = new DefaultInternalRouting(net,discovery);
        this.services = new ServiceHandler(lookup,internalRouting);
        //clientNetwork = net;
        //services = new DeprecatedServiceHandler(lookup, network);
        //TODO swap for current versions
        this.network = net;
        }
    
    public Cotton (boolean globalServiceDiscovery, int portNumber) throws java.net.UnknownHostException {
        //TODO swap for current versions
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        NetworkHandler net = null;
        if(globalServiceDiscovery) {
            net = new DefaultNetworkHandler(portNumber);
            discovery = new GlobalServiceDiscovery(globalDiscoveryDNS);
        
        }else {
            net = new DefaultNetworkHandler(portNumber);
            discovery = new LocalServiceDiscovery(globalDiscoveryDNS);
        
        }
        lookup = new ServiceLookup();
        discovery.setLocalServiceTable(lookup);
        this.internalRouting = new DefaultInternalRouting(net,discovery);
        this.services = new ServiceHandler(lookup,internalRouting);
        this.network = net;
        
    }
    /*  
    public Cotton () throws java.net.UnknownHostException {
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
        NetworkHandler net = new DefaultNetworkHandler();
        network = net;
        //clientNetwork = net;
        //services = new DeprecatedServiceHandler(lookup, network);
        //TODO swap for current versions
    }
*/
    public void start(){
        new Thread(network).start();
        internalRouting.start();
        new Thread(services).start();
        discovery.announce();
    }

    public void shutdown() {
        services.stop();
        discovery.stop();
        network.stop();
        internalRouting.stop();

    }

    public ActiveServiceLookup getServiceRegistation() {
        return lookup;
    }

    public NetworkHandler getNetwork() {
        return network;
    }

    public InternalRoutingClient getClient(){
        return internalRouting;
    }

    public static void main(String[] args) {
        Cotton c = null;
        try{
            c = new Cotton(true,3333);
        }catch(java.net.UnknownHostException e){// TODO: Rethink this
            System.out.println("Init network error, exiting");
            return;
        }finally{
            c.shutdown();
        }
    }
}
