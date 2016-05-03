package cotton.network;

import java.io.Serializable;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The <code>DummyServiceChain</code> implements the <code>ServiceChain</code> 
 * interface and maintains the chain of services to be executed.
 * 
 * @author Magnus
 * @see ServiceChain
 */
public class DummyServiceChain implements ServiceChain, Serializable {
    private static final long serialVersionUID = 1L;
    private ConcurrentLinkedQueue<String> chain;

    /**
     * Constructs an empty <code>ServiceChain</code> with a concurrent linked queue.
     */
    public DummyServiceChain() {
        this.chain = new ConcurrentLinkedQueue<>();
    }

    /**
     * Constructs a <code>ServiceChain</code> with a given service name. The 
     * <code>ServiceChain</code> is constructed with a concurrent linked queue.
     * 
     * @param serviceName the service name to add to the <code>serviceChain</code>.
     */
    public DummyServiceChain(String serviceName) {
        this.chain = new ConcurrentLinkedQueue<>();
        chain.add(serviceName);
    }

    /**
     * Adds a service name into the <code>ServiceChain</code>.
     * 
     * @param name the service name.
     * @return <code>true</code> if successful.
     */
    @Override
    public boolean addService(String name) {
        chain.add(name);
        return true;
    }

    /**
     * Removes the service name and returns it from the queue.
     * 
     * @return the next service name.
     */
    @Override
    public String getNextServiceName() {
        return chain.poll();
    }

    /**
     * Peeks at the next service name in the queue and returns it. The service 
     * will not be removed from the queue.
     * 
     * @return the next service name.
     */
    @Override
    public String peekNextServiceName() {
        return chain.peek();
    }

    /**
     * Adds a service to the service queue and returns this object. This allows 
     * more than one add in a single command.
     * 
     * @param name the service to add to the queue.
     * @return <code>this</code> object.
     */
    @Override
    public ServiceChain into(String name) {
        chain.add(name);
        return this;
    }

    /**
     * Returns a <code>String</code> containing the different service names.
     * 
     * @return the different service names.
     */
    @Override
    public String toString(){
        return chain.toString();
    }
}
