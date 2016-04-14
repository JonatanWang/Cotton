package cotton;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class SerializableImage extends BufferedImage implements Serializable {

    public SerializableImage (int width, int height, int type) {
        super(width, height, type);
    }

    public static SerializableImage serializeImage(Image img){
        SerializableImage image = new SerializableImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        Graphics2D imageGraphics = image.createGraphics();
        imageGraphics.drawImage(img, 0, 0, null);
        imageGraphics.dispose();

        return image;
    }
}
