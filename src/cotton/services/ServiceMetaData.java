package cotton.services;

import java.util.concurrent.atomic.AtomicInteger;


/**
 *
 *
 * @author Tony
 * @author Magnus
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
