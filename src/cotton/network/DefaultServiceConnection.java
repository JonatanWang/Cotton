
package cotton.network;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Random;

import cotton.services.ServiceConnection;

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
    public SocketAddress getAddress() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setAddress(SocketAddress addr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
