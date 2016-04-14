package cotton;

import cotton.network.ClientNetwork;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import cotton.network.DefaultNetworkHandler;
import cotton.network.NetworkHandler;
import cotton.services.ActiveServiceLookup;
import cotton.services.DefaultActiveServiceLookup;
import cotton.services.DummyServiceChain;
import cotton.services.FileWriterService;
import cotton.services.ImageManipulationPacket;
import cotton.services.ImageManipulationService;
import cotton.services.ServiceChain;
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
    private Thread th;

    public Cotton () {
        lookup = new DefaultActiveServiceLookup();
        DefaultNetworkHandler net = new DefaultNetworkHandler();
        network = net;
        clientNetwork = net;
        services = new ServiceHandler(lookup, network);
    }

    public void start(){
        th = new Thread(new ServiceThread());
        th.start();
    }

    public void shutdown() {
        services.stop();
       // th.stop();
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

    private class ServiceThread implements Runnable {
        @Override
        public void run() {
            services.start();
        }
    }

    public static void main(String[] args) {
        Cotton c = new Cotton();

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

        c.getNetwork().sendServiceResult(null, new ImageManipulationPacket(i), s);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException ignore) { }

        c.shutdown();
    }
}
