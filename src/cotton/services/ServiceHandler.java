package cotton.services;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.Serializable;
import cotton.network.DeprecatedNetworkHandler;

public class ServiceHandler implements Runnable{
    private ActiveServiceLookup serviceLookup;
    private DeprecatedNetworkHandler networkHandler;
    private ExecutorService threadPool;
    private volatile boolean active = true;

    public ServiceHandler(ActiveServiceLookup serviceLookup, DeprecatedNetworkHandler networkHandler){
        this.networkHandler = networkHandler;
        this.serviceLookup = serviceLookup;
        threadPool = Executors.newCachedThreadPool();
    }

    public void run(){
        while(active){
            ServicePacket packet = networkHandler.nextPacket();
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
        private ServiceFactory serviceFactory;
        private boolean succesfulInit = true;
        private ServicePacket servicePacket;
        private String serviceName;
        public ServiceDispatcher(ServicePacket servicePacket){
            this.servicePacket = servicePacket;
            this.serviceName = this.servicePacket.getTo().getNextServiceName();

            if(this.serviceName == null){
                succesfulInit = false;
                return;
            }

            ServiceMetaData serviceMetaData = serviceLookup.getService(serviceName);
            if(serviceMetaData == null){
                succesfulInit = false;
                return;
            }
            int currentServiceCount = serviceMetaData.serviceStarted();
            if(currentServiceCount > serviceMetaData.getMaxCapacity()){
                serviceMetaData.serviceFinished();
                succesfulInit = false;
                return;
            }

            this.serviceFactory = serviceMetaData.getServiceFactory();

        }
        @Override
        public void run(){
            if(succesfulInit == false)
                return;
            ServiceInstance service = serviceFactory.newServiceInstance();
            try{

                Serializable result = service.consumeServiceOrder(null,servicePacket.getFrom(),servicePacket.getDataStream(),servicePacket.getTo());
                networkHandler.sendToService(result, servicePacket.getTo(),servicePacket.getFrom());

            }catch(Exception e){
                e.printStackTrace();
            }
            serviceLookup.getService(serviceName).serviceFinished();
        }
    }
}
