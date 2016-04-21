package cotton;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import cotton.example.FileWriterService;
import cotton.example.ImageManipulationPacket;
import cotton.example.ImageManipulationService;
import cotton.network.ClientNetwork;
import cotton.network.DefaultNetworkHandler;
import cotton.network.NetworkHandler;
import cotton.network.ServiceChain;
import cotton.servicediscovery.DefaultLocalServiceDiscovery;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.services.ActiveServiceLookup;
import cotton.services.DefaultActiveServiceLookup;
import cotton.network.DummyServiceChain;
import cotton.services.ServiceHandler;

/**
 *
 * @author Jonathan
 * @author Magnus
 */
public class Cotton {
    private ActiveServiceLookup lookup;
    private NetworkHandler network;
    private ClientNetwork clientNetwork;
    private ServiceHandler services;
    private LocalServiceDiscovery discovery;

    public Cotton () throws java.net.UnknownHostException{
        lookup = new DefaultActiveServiceLookup();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        this.discovery = new DefaultLocalServiceDiscovery(lookup,globalDiscoveryDNS);
        DefaultNetworkHandler net = new DefaultNetworkHandler(discovery);
        network = net;
        clientNetwork = net;
        services = new ServiceHandler(lookup, network);
    }

    public void start(){
        new Thread(services).start();
    }

    public void shutdown() {
        services.stop();
    }

    public ActiveServiceLookup getServiceRegistation() {
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

        c.getNetwork().sendToService(new ImageManipulationPacket(i), s,null);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignore) { }

        c.shutdown();
    }
}
