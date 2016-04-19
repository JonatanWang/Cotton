package cotton.example;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class FileWriterService implements ServiceInstance{

    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data,
                                            ServiceChain to) {

        System.out.println("Write");

        try {
            ObjectInputStream inStream = new ObjectInputStream(data);
            BufferedImage image = ((ImageManipulationPacket)inStream.readObject()).getImage(); 
            ImageIO.write(image, "png", new File("test.png"));
        }catch (IOException ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }catch (ClassNotFoundException ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return "null";
    }

    public static ServiceFactory getFactory(){
        return new FileFactory();
    }

    public static class FileFactory implements ServiceFactory {

        @Override
        public ServiceInstance newServiceInstance() {
            return new FileWriterService();
        }

    }

}

