package cotton.services;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.InputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;

/**
 * Service that manipulates an incoming image
 *
 * @author Jonathan
 * @author Mats
 */
public class ImageManipulationService implements ServiceInstance{

    @Override
    public Serializable consumeServiceOrder(CloudContext ctx, ServiceConnection from, InputStream data,
                                            ServiceChain to) {

        BufferedImage image = null;

        try {
            image = ImageIO.read(data);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }

        image = invertColors(image);
        image = applyText(image, 100, 100, new Font("Arial", Font.PLAIN, 30), "Amazing");

        SerializableImage serializableimage = SerializableImage.serializeImage(image);

        return serializableimage;
    }

    private static BufferedImage invertColors(BufferedImage image) {
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

    private static BufferedImage tintImage(BufferedImage image, int r, int g, int b) {
        //TODO: fix tint
        return null;
    }

    public static BufferedImage applyText(BufferedImage image, int x, int y, Font fnt, String text){
        Graphics g = image.getGraphics();
        g.setFont(fnt);
        g.drawString(text, x, y);
        g.dispose();

        return image;
    }
}
