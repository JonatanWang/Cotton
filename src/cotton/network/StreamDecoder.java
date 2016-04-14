
package cotton.network;

import java.io.InputStream;

/**
 *
 * @author Magnus
 */
public interface StreamDecoder <T> {
    public T decode(InputStream input);
}
