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
@Deprecated
public class DeprecatedServiceMetaData {
    private DeprecatedServiceFactory serviceFactory;
    private int maxCapacity;
    private AtomicInteger currentThreadCount;

    /**
     * Constructs a object with information about the service. The thread count
     * will start at zero.
     * 
     * @param serviceFactory the <code>ServiceFactory</code> for the service.
     * @param maxCapacity defines the maximum amount of simultaneously services.
     */
    public DeprecatedServiceMetaData(DeprecatedServiceFactory serviceFactory, int maxCapacity) {
            this.maxCapacity = maxCapacity;
            this.serviceFactory = serviceFactory;
            currentThreadCount = new AtomicInteger();
    }

    /**
     * Increments the number keeping track of running instances. This function
     * should be executed when a new thread is launched.
     * 
     * @return the number of instances running.
     */
    public int incrementThreadCount(){
            return currentThreadCount.incrementAndGet();
    }

    /**
     * Decrements the number keeping track of running instances. This function
     * should be executed when a thread is turned off.
     * 
     * @return the number of instances running.
     */
    public int decrementThreadCount(){
            return currentThreadCount.decrementAndGet();
    }

    /**
     * Returns the maximum amount of instances that are allowed to run 
     * simultaneously.
     * 
     * @return maximum capacity of the service.
     */
    public int getMaxCapacity() {
            return maxCapacity;
    }

    /**
     * Returns the current amount of instances running in the thread pool.
     * 
     * @return current amount of instances.
     */
    public int getCurrentThreadCount(){
            return currentThreadCount.get();
    }

    /**
     * Returns the <code>ServiceFactory</code> connected to the service.
     * 
     * @return the <code>ServiceFactory</code> connected to the service.
     */
    public DeprecatedServiceFactory getServiceFactory(){
            return serviceFactory;
    }
}
