package cotton.servicediscovery;

import java.net.SocketAddress;
import java.util.ArrayList;

/**
 *
 * @author Magnus, Mats
 */
class AddressPool {

    private int pos = 0;
    private ArrayList<SocketAddress> pool = new ArrayList<SocketAddress>();

    public boolean addAddress(SocketAddress address) {
        synchronized (this) {
            pool.add(address);
        }
        return true;
    }

    public SocketAddress getAddress() {
        SocketAddress addr = null;
        
        synchronized (this) {
            if (pool.isEmpty() == false) {
                pos = pos % pool.size();
                addr = pool.get(pos);
                pos++;
            }
        }
        return addr;
    }
}
