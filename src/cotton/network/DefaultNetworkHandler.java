package cotton.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.servicediscovery.RouteSignal;
import cotton.services.DefaultServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;

/**
 *
 * @author Magnus
 * @author Tony
 * @author Jonathan
 * @author Gunnlaugur
 */
// TODO: fix todos
public class DefaultNetworkHandler implements NetworkHandler,ClientNetwork {
    private ServiceBuffer serviceBuffer;
    private ConcurrentHashMap<UUID,DefaultServiceRequest> connectionTable;
    private AtomicBoolean running;
    private LocalServiceDiscovery localServiceDiscovery;
    public DefaultNetworkHandler(LocalServiceDiscovery localServiceDiscovery) {
        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<>();
        this.localServiceDiscovery = localServiceDiscovery;
        localServiceDiscovery.setNetwork(this,null); // TODO: get socket address

    }
 
	@Override
	public ServiceRequest send(Serializable result, ServiceConnection destination) {
		// TODO Auto-generated method stub
      Socket socket = new Socket(); // TODO: Encryption
      DefaultServiceRequest returnValue = new DefaultServiceRequest();
      this.connectionTable.put(destination.getUserConnectionId(), returnValue);
      try {
          socket.connect(destination.getAddress());
          new ObjectOutputStream(socket.getOutputStream()).writeObject(result);
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
      return returnValue;
  }

	@Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    @Override
    public void sendToService(Serializable result, ServiceChain to,ServiceConnection from) {
        // TODO: user service discovery and switch on RouteSignal
        if(to.getCurrentServiceName() != null){
            DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(from,result,to);
            bufferStuffer.fillBuffer();
        }else if(from != null) {
            DefaultServiceRequest req = connectionTable.get(from.getUserConnectionId());
            if(req == null) return; // TODO: dont drop results, and send data to service discovary
            req.setData(result);
        }        
    }


    @Override
    public ServiceRequest sendToService(Serializable data, ServiceChain to) {
        //DefaultServiceRequest result = new DefaultServiceRequest();
        UUID uuid = UUID.randomUUID();
        //this.connectionTable.put(uuid,result);
        ServiceConnection dest = new DefaultServiceConnection(uuid);
        RouteSignal route = localServiceDiscovery.getDestination(dest, to);
        if(route == RouteSignal.LOCALDESTINATION) {
            DefaultServiceRequest result = new DefaultServiceRequest();
            this.connectionTable.put(uuid,result);
            DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(dest,data,to);
            bufferStuffer.fillBuffer();
            return result;
        }
        return send(data, dest);
    }

    /*
    @Override
    public Serializable getResults(ServiceConnection requestId, StreamDecoder decoder) {
        //TODO: implement decoder call
        ServiceRequest req = this.connectionTable.get(requestId.getUserConnectionId());
        return req.getData();
    }*/

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
