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
import cotton.servicediscovery.DeprecatedServiceDiscovery;
import cotton.services.DeprecatedActiveServiceLookup;
import cotton.services.ServiceHandler;
/**
 *
 * @author Jonathan
 * @author Magnus
 * @author Gunnlaugur
 */
public class Cotton {
    private DeprecatedActiveServiceLookup lookup;
    private NetworkHandler network;
    private ClientNetwork clientNetwork;
    private ServiceHandler services;
    private DeprecatedServiceDiscovery discovery;

    public Cotton (boolean GlobalServiceDiscovery) throws java.net.UnknownHostException {
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        NetworkHandler net = null;
        if(GlobalServiceDiscovery) {
            this.discovery = new DefaultGlobalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DefaultNetworkHandler();
        }else {
            this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DefaultNetworkHandler(ThreadLocalRandom.current().nextInt(3000,20000));
        }
        network = net;
        //clientNetwork = net;
        //services = new DeprecatedServiceHandler(lookup, network);
        //TODO swap for current versions
    }
    
    public Cotton (boolean GlobalServiceDiscovery, int portNumber) throws java.net.UnknownHostException {
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        NetworkHandler net = null;
        if(GlobalServiceDiscovery) {
            this.discovery = new DefaultGlobalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DefaultNetworkHandler();
        }else {
            this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DefaultNetworkHandler(portNumber);
        }
        network = net;
        //clientNetwork = net;
        //services = new DeprecatedServiceHandler(lookup, network);
        //TODO swap for current versions
    }
    
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

    public void start(){
        new Thread(services).start();
        new Thread(network).start();
        discovery.announce();
    }

    public void shutdown() {
        services.stop();
        discovery.stop();
        network.stop();
    }

    public DeprecatedActiveServiceLookup getServiceRegistation() {
        return lookup;
    }

    public NetworkHandler getNetwork() {
        return network;
    }
    
    public ClientNetwork getClientNetwork() {
        return this.clientNetwork;
    }

    public static void main(String[] args) {
        Cotton c;
        try{
            c = new Cotton();
        }catch(java.net.UnknownHostException e){// TODO: Rethink this
            System.out.println("Init network error, exiting");
            return;
        }

        c.getServiceRegistation().registerService("ImageService", ImageManipulationService.getFactory(), 8);
        c.getServiceRegistation().registerService("FileWriter", FileWriterService.getFactory(), 1);

        ServiceChain s = new DummyServiceChain("ImageService");

        s.addService("FileWriter");

        BufferedImage i = null;

        try {
            i = ImageIO.read(new File("test_image.png"));
        }catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        c.start();

        //        c.getNetwork().sendToService(new ImageManipulationPacket(i), s, null);

        try {
            Thread.sleep(50000);
        } catch (InterruptedException ignore) { }
   
        c.shutdown();
    }
}
