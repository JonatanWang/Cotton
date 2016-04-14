package cotton.services;

import java.util.concurrent.Executors;
import java.io.PipedOutputStream;
import java.io.PipedInputStream;
import java.util.concurrent.ExecutorService;
import java.io.Serializable;
import java.io.ObjectOutputStream;
import java.io.IOException;

public class ServiceHandler{
    private ActiveServiceLookup serviceLookup;
    private ServiceBuffer serviceBuffer;
    private ExecutorService threadPool;
    private boolean active = true;

    public ServiceHandler(ActiveServiceLookup serviceLookup, ServiceBuffer serviceBuffer){
        this.serviceBuffer = serviceBuffer;
        this.serviceLookup = serviceLookup;
        threadPool = Executors.newCachedThreadPool();
    }

    public void start(){
        while(active){
            ServicePacket packet = serviceBuffer.nextPacket();
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
    }

    public void stop(){
        this.active = false;
    }

    private class ServiceDispatcher implements Runnable{
        private ServiceFactory serviceFactory;
        private boolean succesfulInit = true;
        private ServicePacket servicePacket;
        String serviceName;
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
            if(currentServiceCount >= serviceMetaData.getMaxCapacity()){
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
                
                Serializable res = service.consumeServiceOrder(null,servicePacket.getFrom(),servicePacket.getDataStream(),servicePacket.getTo());

                if(this.servicePacket.getTo().getCurrentServiceName() != null){
                    DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(this.servicePacket.getFrom(),res,this.servicePacket.getTo());
                    bufferStuffer.fillBuffer();
                }
            }catch(Exception e){
                e.printStackTrace();
            }
            serviceLookup.getService(serviceName).serviceFinished();
        }
    }

    private class DummyBufferStuffer{
        private ServicePacket servicePacket;
        public DummyBufferStuffer(ServiceConnection from, Serializable data, ServiceChain to){
            try{
                PipedInputStream in = new PipedInputStream();
                PipedOutputStream outStream = new PipedOutputStream(in);
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                objectOutStream.writeObject(data);
                objectOutStream.close();
            
                this.servicePacket = new ServicePacket(from,in,to);
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        public void fillBuffer(){
            serviceBuffer.add(this.servicePacket);
        }
        

    }

}
