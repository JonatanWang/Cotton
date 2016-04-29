package cotton;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import cotton.example.FileWriterService;
import cotton.example.ImageManipulationPacket;
import cotton.example.ImageManipulationService;
import cotton.network.ClientNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.DeprecatedDefaultNetworkHandler;
import cotton.network.ServiceChain;
import cotton.servicediscovery.DefaultLocalServiceDiscovery;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.services.DefaultActiveServiceLookup;
import cotton.network.DummyServiceChain;
import cotton.servicediscovery.DefaultGlobalServiceDiscovery;
import cotton.services.DeprecatedServiceHandler;
import java.util.concurrent.ThreadLocalRandom;
import cotton.network.DeprecatedNetworkHandler;
import cotton.servicediscovery.DeprecatedServiceDiscovery;
import cotton.services.DeprecatedActiveServiceLookup;
/**
 *
 * @author Jonathan
 * @author Magnus
 * @author Gunnlaugur
 */
public class DeprecatedCotton {
    private DeprecatedActiveServiceLookup lookup;
    private DeprecatedDefaultNetworkHandler network;
    private ClientNetwork clientNetwork;
    private DeprecatedServiceHandler services;
    private DeprecatedServiceDiscovery discovery;

    public DeprecatedCotton (boolean GlobalServiceDiscovery) throws java.net.UnknownHostException {
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        DeprecatedDefaultNetworkHandler net = null;
        if(GlobalServiceDiscovery) {
            this.discovery = new DefaultGlobalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DeprecatedDefaultNetworkHandler(discovery);
        }else {
            this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DeprecatedDefaultNetworkHandler(discovery,ThreadLocalRandom.current().nextInt(3000,20000));
        }
        network = net;
        clientNetwork = net;
        services = new DeprecatedServiceHandler(lookup, network);
    }
    
    public DeprecatedCotton (boolean GlobalServiceDiscovery, int portNumber) throws java.net.UnknownHostException {
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        DeprecatedDefaultNetworkHandler net = null;
        if(GlobalServiceDiscovery) {
            this.discovery = new DefaultGlobalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DeprecatedDefaultNetworkHandler(discovery);
        }else {
            this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
            net = new DeprecatedDefaultNetworkHandler(discovery, portNumber);
        }
        network = net;
        clientNetwork = net;
        services = new DeprecatedServiceHandler(lookup, network);
    }
    
    public DeprecatedCotton () throws java.net.UnknownHostException {
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
        DeprecatedDefaultNetworkHandler net = new DeprecatedDefaultNetworkHandler(discovery);
        network = net;
        clientNetwork = net;
        services = new DeprecatedServiceHandler(lookup, network);
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

    public DeprecatedNetworkHandler getNetwork() {
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
