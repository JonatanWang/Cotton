
package cotton.network;

import java.io.InputStream;
import java.io.Serializable;

/**
 *
 * @author Magnus
 */
public interface StreamDecoder{
    public Serializable decode(InputStream input);
}
