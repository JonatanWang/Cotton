
package cotton.network;

import cotton.services.ServiceConnection;
import java.net.InetAddress;
import java.util.Random;

/**
 *
 * @author Magnus
 */
public class DefaultServiceConnection implements ServiceConnection {
    private Integer conId;
    private String name;

    public DefaultServiceConnection() {
        conId = new Integer(new Random().nextInt());
        this.name = "none";
    }
    public DefaultServiceConnection(String name) {
        conId = new Integer(new Random().nextInt());
        this.name = name;
    }
    
    @Override
    public Integer getUserConnectionId() {
        return this.conId;
    }

    @Override
    public String getServiceName() {
        return this.name;
    }

    @Override
    public InetAddress getAddress() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAddress(InetAddress addr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
