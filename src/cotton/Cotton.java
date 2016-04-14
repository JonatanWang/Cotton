package cotton;

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
    private ServiceHandler services;

    public Cotton () {
        lookup = new DefaultActiveServiceLookup();
        network = new DefaultNetworkHandler();
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
