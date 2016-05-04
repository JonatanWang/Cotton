package cotton.services;

import cotton.internalRouting.InternalRoutingServiceHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.Serializable;

public class ServiceHandler implements Runnable{
    private ActiveServiceLookup serviceLookup;
    private InternalRoutingServiceHandler internalRouting;
    private ServiceBuffer workBuffer;
    private ExecutorService threadPool;
    private volatile boolean active = true;

    public ServiceHandler(ActiveServiceLookup serviceLookup, InternalRoutingServiceHandler internalRouting){
        this.internalRouting = internalRouting;
        this.serviceLookup = serviceLookup;
        this.workBuffer = internalRouting.getServiceBuffer();
        threadPool = Executors.newCachedThreadPool();
    }

    public void run(){
        while(active){
            ServicePacket packet = workBuffer.nextPacket();
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
            Service service = serviceFactory.newService();
            try{

                byte[] result = service.execute(null, servicePacket.getOrigin(), servicePacket.getData(),servicePacket.getTo());
                internalRouting.forwardResult(servicePacket.getOrigin(), servicePacket.getTo(), result);
                serviceLookup.getService(serviceName).decrementThreadCount(); 
                internalRouting.notifyRequestQueue(serviceName);
                
            }catch(Exception e){
                serviceLookup.getService(serviceName).decrementThreadCount(); 
                e.printStackTrace();
            }
        }
    }
}
