package cotton.example;

import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.network.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class FileWriterService implements ServiceInstance{

    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data, ServiceChain to) {
        System.out.println("Write");
        BufferedImage image = null;

        ImageManipulationPacket input = null;
        try{
            input = (ImageManipulationPacket)new ObjectInputStream(data).readObject();
            image = bytesToBufferedImage(input.getImage());

            ImageIO.write(image, "jpg", new File("test.jpg"));
        }catch (IOException ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }catch (ClassNotFoundException e){
            System.out.println(e.getMessage());
        }

        return input;
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

    private BufferedImage bytesToBufferedImage(byte[] serializedImage){
        InputStream in = new ByteArrayInputStream(serializedImage);
        BufferedImage image = null;
        try {
            image = ImageIO.read(in);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        return image;
    }

    private byte[] bufferedImageToBytes(BufferedImage image){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", output);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        return output.toByteArray();
    }

}

