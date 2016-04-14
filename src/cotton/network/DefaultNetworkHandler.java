package cotton.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import cotton.services.DefaultServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Magnus
 */
public class DefaultNetworkHandler implements NetworkHandler,ClientNetwork {
    private ServiceBuffer serviceBuffer;
    private ConcurrentHashMap<Integer,ServiceRequest> connectionTable;
    
    public DefaultNetworkHandler() {
        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<Integer,ServiceRequest>();
    }
    
    private class ServiceRequest {
        private Serializable data = null;
        private CountDownLatch latch = new CountDownLatch(1);
        
        public Serializable getData() {
            boolean loop = false;
            do {
                try {
                    latch.await();
                    loop = false;
                } catch (InterruptedException ex) {loop = true;}
            }while(loop);
            return data;
        }

        public void setData(Serializable data) {
            this.data = data;
            latch.countDown();
        }
        
    }
    
    @Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    @Override
    public void sendServiceResult(ServiceConnection from, Serializable result, ServiceChain to) {
        
        if(to.getCurrentServiceName() != null){
            DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(from,result,to);
            bufferStuffer.fillBuffer();
        }else if(from != null) {
            ServiceRequest req = connectionTable.get(from.getUserConnectionId());
            if(req == null) return; // TODO: dont drop results, and send data to service discovary
            req.setData(result);
        }        
    }

    
    @Override
    public ServiceConnection sendServiceRequest(Serializable data, ServiceChain to) {
        ServiceConnection from = new DefaultServiceConnection();
        this.connectionTable.put(from.getUserConnectionId(), new ServiceRequest());
        DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(from,data,to);
        bufferStuffer.fillBuffer();
        return from;
    }

    @Override
    public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder) {
        //TODO: implement decoder call
        ServiceRequest req = this.connectionTable.get(requestId.getUserConnectionId());
        return req.getData();
    }
    
    private class DummyBufferStuffer{
        private ServicePacket servicePacket;
        public DummyBufferStuffer(ServiceConnection from, Serializable data, ServiceChain to){
            try{
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);


                oos.writeObject(data);

                oos.flush();
                oos.close();

                InputStream in = new ByteArrayInputStream(baos.toByteArray());
                /*
                System.out.println("1");
                PipedOutputStream outStream = new PipedOutputStream();
                System.out.println("2");
                PipedInputStream in = new PipedInputStream(outStream);
                System.out.println("3");
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                System.out.println("4");
                objectOutStream.writeObject(data);
                System.out.println("5");
                objectOutStream.flush();
                objectOutStream.close();*/

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
