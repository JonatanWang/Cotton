package cotton.example;

import cotton.network.Origin;
import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.imageio.ImageIO;

/**
 * Service that manipulates an incoming image
 *
 * @author Jonathan
 * @author Mats
 */
public class ImageManipulationService implements Service {

    private ImageManipulationService () {
    }

    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
        BufferedImage image = null;

        System.out.println("Manipulation");

        try{
            image = bytesToBufferedImage(data);

            image = invertColors(image);
            image = applyText(image, 100, 100, new Font("Arial", Font.PLAIN, 30), "Amazing");
        }catch (Exception ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return bufferedImageToBytes(image);
    }

    private BufferedImage invertColors(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        WritableRaster raster = image.getRaster();
        int[] pixels = null;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                pixels = raster.getPixel(x, y, (int[]) null);
                if(pixels.length > 3){
                    if(pixels[3] != 0){
                        pixels[0] = 255-pixels[0];
                        pixels[1] = 255-pixels[1];
                        pixels[2] = 255-pixels[2];
                        raster.setPixel(x, y, pixels);
                    }
                }else{
                    pixels[0] = 255-pixels[0];
                    pixels[1] = 255-pixels[1];
                    pixels[2] = 255-pixels[2];
                    raster.setPixel(x, y, pixels);
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
        public Service newService() {
            return new ImageManipulationService();
        }

    }

    private BufferedImage bytesToBufferedImage(byte[] serializedImage){
        InputStream in = new ByteArrayInputStream(serializedImage);
        BufferedImage image = null;
        try {
            image = ImageIO.read(in);
        }catch (Throwable e) {
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
