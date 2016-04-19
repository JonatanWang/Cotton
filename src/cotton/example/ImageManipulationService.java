package cotton.example;

import cotton.services.CloudContext;
import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServiceFactory;
import cotton.services.ServiceInstance;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;

/**
 * Service that manipulates an incoming image
 *
 * @author Jonathan
 * @author Mats
 */
public class ImageManipulationService implements ServiceInstance{

    private ImageManipulationService () {
        
    }

    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data,
                                            ServiceChain to) {

        BufferedImage image = null;

        System.out.println("Manipulation");

        ObjectInputStream inStream;
        try {
            inStream = new ObjectInputStream(data);
            image = ((ImageManipulationPacket)inStream.readObject()).getImage();
        }catch (IOException ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }catch (ClassNotFoundException ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }

        image = invertColors(image);
        image = applyText(image, 100, 100, new Font("Arial", Font.PLAIN, 30), "Amazing");

        return new ImageManipulationPacket(image);
    }

    private BufferedImage invertColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = image.getRaster();
        int[] pixels = null;

        for (int xx = 0; xx < width; xx++) {
            for (int yy = 0; yy < height; yy++) {
                pixels = raster.getPixel(xx, yy, (int[]) null);
                if(pixels[3] != 0){
                    pixels[0] = 255-pixels[0];
                    pixels[1] = 255-pixels[1];
                    pixels[2] = 255-pixels[2];
                    raster.setPixel(xx, yy, pixels);
                }
            }
        }
        return image;
    }

    private BufferedImage tintImage(BufferedImage image, int r, int g, int b) {
        //TODO: fix tint
        return null;
    }

    public BufferedImage applyText(BufferedImage image, int x, int y, Font fnt, String text){
        Graphics g = image.getGraphics();
        g.setFont(fnt);
        g.drawString(text, x, y);
        g.dispose();

        return image;
    }

    public static ServiceFactory getFactory(){
        return new ImageFactory();
    }

    public static class ImageFactory implements ServiceFactory {

        private ImageFactory () {

        }

        @Override
        public ServiceInstance newServiceInstance() {
            return new ImageManipulationService();
        }

    }
}
