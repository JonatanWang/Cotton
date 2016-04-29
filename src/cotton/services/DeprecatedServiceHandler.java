package cotton.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.Serializable;
import cotton.network.DeprecatedNetworkHandler;

public class DeprecatedServiceHandler implements Runnable{
    private DeprecatedActiveServiceLookup serviceLookup;
    private DeprecatedNetworkHandler networkHandler;
    private ExecutorService threadPool;
    private volatile boolean active = true;

    public DeprecatedServiceHandler(DeprecatedActiveServiceLookup serviceLookup, DeprecatedNetworkHandler networkHandler){
        this.networkHandler = networkHandler;
        this.serviceLookup = serviceLookup;
        threadPool = Executors.newCachedThreadPool();
    }

    public void run(){
        while(active){
            DeprecatedServicePacket packet = networkHandler.nextPacket();
            if(packet == null){
                try{
                    Thread.sleep(5); //change to exponential fallback strategy.
                }catch(InterruptedException ex){
                }
            }else{
                ServiceDispatcher th = new ServiceDispatcher(packet);
                threadPool.execute(th);
            }
        }
        threadPool.shutdown();
    }

    public void stop(){
        this.active = false;
    }

    private class ServiceDispatcher implements Runnable{
        private DeprecatedServiceFactory serviceFactory;
        private boolean succesfulInit = true;
        private DeprecatedServicePacket servicePacket;
        private String serviceName;
        public ServiceDispatcher(DeprecatedServicePacket servicePacket){
            this.servicePacket = servicePacket;
            this.serviceName = this.servicePacket.getTo().getNextServiceName();

            if(this.serviceName == null){
                succesfulInit = false;
                return;
            }

            DeprecatedServiceMetaData serviceMetaData = serviceLookup.getService(serviceName);
            if(serviceMetaData == null){
                succesfulInit = false;
                return;
            }
            int currentServiceCount = serviceMetaData.incrementThreadCount();
            if(currentServiceCount > serviceMetaData.getMaxCapacity()){
                serviceMetaData.decrementThreadCount();
                succesfulInit = false;
                return;
            }

            this.serviceFactory = serviceMetaData.getServiceFactory();

        }
        @Override
        public void run(){
            if(succesfulInit == false)
                return;
            DeprecatedService service = serviceFactory.newService();
            try{

                byte[] result = service.execute(null, servicePacket.getFrom(), servicePacket.getData(),servicePacket.getTo());
                networkHandler.sendToService(result, servicePacket.getTo(), servicePacket.getFrom());

            }catch(Exception e){
                e.printStackTrace();
            }
            serviceLookup.getService(serviceName).decrementThreadCount();
        }
    }
}
