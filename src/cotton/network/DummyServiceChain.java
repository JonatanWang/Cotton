package cotton.network;

import cotton.network.ServiceChain;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author Magnus
 */
public class DummyServiceChain implements ServiceChain {
    private ConcurrentLinkedQueue<String> chain;
    
    public DummyServiceChain() {
        this.chain = new ConcurrentLinkedQueue<String>();
    }
    
    public DummyServiceChain(String serviceName) {
        this.chain = new ConcurrentLinkedQueue<String>();
        chain.add(serviceName);
    }

    @Override
    public boolean addService(String name) {
        chain.add(name);
        return true;
    }

    @Override
    public String getNextServiceName() {
        return chain.poll();
    }

    @Override
    public String peekNextServiceName() {
        return chain.peek();
    }

    @Override
    public ServiceChain into(String name) {
        chain.add(name);
        return this;
    }

}
