package cotton.services;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The <code>ServiceMetaData</code> consists of information about the service. 
 * The <code>ServiceHandler</code> needs to manually update the thread count for
 * the <code>currentThreadCount</code> to work.
 *
 * @author Tony
 * @author Magnus
 * @see ServiceHandler
 */
public class ServiceMetaData {
    private ServiceFactory serviceFactory;
    private int maxCapacity;
    private AtomicInteger currentThreadCount;

    public ServiceMetaData(ServiceFactory serviceFactory, int maxCapacity) {
            this.maxCapacity = maxCapacity;
            this.serviceFactory = serviceFactory;
            currentThreadCount = new AtomicInteger();
    }

    //TODO GET BETTER NAME
    public int serviceStarted(){
            return currentThreadCount.incrementAndGet();
    }

    public int serviceFinished(){
            return currentThreadCount.decrementAndGet();
    }

    public int getMaxCapacity() {
            return maxCapacity;
    }

    public int getCurrentThreadCount(){
            return currentThreadCount.get();
    }

    public ServiceFactory getServiceFactory(){
            return serviceFactory;
    }
}
