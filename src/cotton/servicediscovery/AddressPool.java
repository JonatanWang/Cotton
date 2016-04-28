package cotton.servicediscovery;

import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * The <code>AddressPool</code> acts as a list of <code>SocketAddresses</code> 
 * that you can add and remove from. The pool is initialized with zero 
 * <code>SocketAddresses</code>.
 * 
 * @author Magnus, Mats
 * @see SocketAddress
 */
public class AddressPool {

    private int pos = 0;
    private ArrayList<SocketAddress> pool = new ArrayList<>();

    /**
     * Adds a <code>SocketAddress</code> to the <code>AddressPool</code> concurrently.
     * 
     * @param address the <code>SocketAddress</code> to add to the <code>AddressPool</code>.
     * @return <code>true</code> always.
     */
    public boolean addAddress(SocketAddress address) {
        synchronized (this) {
            pool.add(address);
        }
        return true;
    }

    /**
     * Returns the next <code>SocketAddress</code> in the <code>AddressPool</code>. 
     * If the <code>AddressPool</code> is empty the function will reply <code>null</code>.
     * 
     * @return the next <code>SocketAddress</code> or null if the list is empty.
     */
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
