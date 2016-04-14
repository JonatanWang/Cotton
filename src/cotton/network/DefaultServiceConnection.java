
package cotton.network;

import cotton.services.ServiceConnection;
import java.util.Random;

/**
 *
 * @author Magnus
 */
public class DefaultServiceConnection implements ServiceConnection {
    private Integer conId;

    public DefaultServiceConnection() {
        conId = new Integer(new Random().nextInt());
    }
    
    @Override
    public Integer getUserConnectionId() {
        return this.conId;
    }
    
}
