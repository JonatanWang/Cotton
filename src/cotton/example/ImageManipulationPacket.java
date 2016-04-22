package cotton.example;

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
    private static final long serialVersionUID = 1L;
    byte[] image;

    public ImageManipulationPacket(byte[] i){
        this.image = i;
    }

    public byte[] getImage(){
        return image;
    }
}
