package mockup;

import java.util.concurrent.atomic.AtomicInteger;

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

        public int serviceFinnished(){
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
