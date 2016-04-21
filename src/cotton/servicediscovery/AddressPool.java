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
            pos = pos % pool.size();

            if (pool.isEmpty() == false) {
                addr = pool.get(pos);
                pos++;
            }
        }
        return addr;
    }
}
