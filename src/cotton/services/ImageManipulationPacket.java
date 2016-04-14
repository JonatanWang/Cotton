package cotton.services;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.imageio.ImageIO;


/**
 * Serializable BufferedImage wrapper
 *
 * @author Jonathan
 * @author Mats
 */
public class ImageManipulationPacket implements Serializable {
    byte[] image;

    public ImageManipulationPacket(BufferedImage i){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(i, "png", baos);
            baos.flush();
            image = baos.toByteArray();
            baos.close();
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
    }

    public BufferedImage getImage(){
        InputStream in = new ByteArrayInputStream(image);
        BufferedImage bimage = null;
        try {
            bimage = ImageIO.read(in);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        return bimage;
    }
}
