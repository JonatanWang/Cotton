package cotton.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import cotton.services.DefaultServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;

/**
 *
 * @author Magnus
 */
public class DefaultNetworkHandler implements NetworkHandler,ClientNetwork {
    private ServiceBuffer serviceBuffer;
    private ConcurrentHashMap<Integer,ServiceRequest> connectionTable;
    private AtomicBoolean running;
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
	public void send(Serializable result, ServiceConnection from) {
		// TODO Auto-generated method stub
      Socket socket = new Socket();
      try {
          socket.connect(from.getAddress());
          new ObjectOutputStream(socket.getOutputStream()).writeObject(result); // TODO: Make less ugly
      }catch (Throwable e) {// TODO: FIX exception
          System.out.println("Error " + e.getMessage());
          e.printStackTrace();
      }finally{
          try {
              socket.close();
          }
          catch (Throwable e) {
              System.out.println("Error " + e.getMessage());
              e.printStackTrace();
          }
      }
	}

	@Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    @Override
    public ServiceConnection sendToService(Serializable result, ServiceChain to,ServiceConnection from) {
        
        if(to.getCurrentServiceName() != null){
            DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(from,result,to);
            bufferStuffer.fillBuffer();
        }else if(from != null) {
            ServiceRequest req = connectionTable.get(from.getUserConnectionId());
            if(req == null) return null; // TODO: dont drop results, and send data to service discovary
            req.setData(result);
        }        
        return null; //TODO: FIX RETURN
    }


    @Override
    public ServiceConnection sendToService(Serializable data, ServiceChain to) {
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

    @Override
    public void run(){
        ServerSocket encryptedServerSocket = null;
        try{
            encryptedServerSocket = new ServerSocket();
        }catch(IOException e){
            e.printStackTrace();
        }
        while(running.get()==true){
            //TODO: Fix networking
        }
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
