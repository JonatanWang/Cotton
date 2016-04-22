package cotton.example;

import java.io.Serializable;


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
