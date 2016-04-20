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
public class DefaultNetworkHandler implements NetworkHandler,ClientNetwork {
    private ServiceBuffer serviceBuffer;
    private ConcurrentHashMap<UUID,DefaultServiceRequest> connectionTable;
    private AtomicBoolean running;
    private LocalServiceDiscovery localServiceDiscovery;

    public DefaultNetworkHandler(LocalServiceDiscovery localServiceDiscovery) {
        this.serviceBuffer = new DefaultServiceBuffer();
        this.connectionTable = new ConcurrentHashMap<>();
        this.localServiceDiscovery = localServiceDiscovery;
        localServiceDiscovery.setNetwork(this, null); // TODO: get socket address
    }

    @Override
    public ServiceRequest send(Serializable result, ServiceConnection destination) {
        Socket socket = new Socket(); // TODO: Encryption
        DefaultServiceRequest returnValue = new DefaultServiceRequest();
        this.connectionTable.put(destination.getUserConnectionId(), returnValue);
        try {
            socket.connect(destination.getAddress());
            new ObjectOutputStream(socket.getOutputStream()).writeObject(result);
        }catch (IOException e) {// TODO: FIX exception & Logger
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }finally{
            try { socket.close(); } catch (Throwable e) {}
        }
        return returnValue;
    }

    @Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    @Override
    public void sendToService(Serializable data, ServiceChain to, ServiceConnection from) {
        ServiceConnection dest = null;
        if(to.peekNextServiceName() != null){
            UUID uuid = UUID.randomUUID();
            dest = new DefaultServiceConnection(uuid);
            RouteSignal route = localServiceDiscovery.getDestination(dest, to);

            if(route == RouteSignal.LOCALDESTINATION) {
                DefaultServiceRequest request = new DefaultServiceRequest();
                this.connectionTable.put(uuid, request);
                sendToServiceBuffer(from, data, to);
                return;
            }

        }else if(from != null){
            DefaultServiceRequest request = connectionTable.get(from.getUserConnectionId());
            if(request == null) return; // TODO: Return the data to the destination
            request.setData(data);
        }

        // TODO: Make sure this sends like expected
        //send(data, dest);
    }


    @Override
    public ServiceRequest sendToService(Serializable data, ServiceChain to) { // TODO: Make sure that this doesn't drop packages with this as last destination
        UUID uuid = UUID.randomUUID();
        ServiceConnection dest = new DefaultServiceConnection(uuid);
        RouteSignal route = localServiceDiscovery.getDestination(dest, to);
        if(route == RouteSignal.LOCALDESTINATION) {
            DefaultServiceRequest result = new DefaultServiceRequest();
            this.connectionTable.put(uuid, result);
            sendToServiceBuffer(dest, data, to);
            return result;
        }
        return send(data, dest);
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

    private void sendToServiceBuffer(ServiceConnection from, Serializable data, ServiceChain to) {
        ServicePacket servicePacket = null;
        try{
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);

            objectStream.writeObject(data);
            objectStream.flush();
            objectStream.close();

            InputStream in = new ByteArrayInputStream(byteStream.toByteArray());

            servicePacket = new ServicePacket(from, in, to);
        }catch(IOException e){
            e.printStackTrace();
        }
        serviceBuffer.add(servicePacket);
    }

}
