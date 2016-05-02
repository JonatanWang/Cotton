
package cotton.servicediscovery;

import cotton.network.DeprecatedDefaultServiceConnection;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import cotton.services.DeprecatedServiceMetaData;
import java.util.UUID;
import cotton.network.PathType;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import cotton.network.DeprecatedNetworkHandler;
import cotton.network.DeprecatedServiceRequest;
import cotton.services.DeprecatedActiveServiceLookup;
import cotton.network.DeprecatedServiceConnection;

/**
 *
 * @author Magnus, Mats
 */
public class DefaultLocalServiceDiscovery implements DeprecatedServiceDiscovery {
    private DeprecatedActiveServiceLookup internalLookup;
    private DeprecatedNetworkHandler network = null;
    private SocketAddress localAddress;
    private ConcurrentHashMap<String, AddressPool> serviceCache;
    private AddressPool globalDiscovery;

    

    private void initGlobalDiscoveryPool(GlobalDiscoveryDNS globalDNS) {
        this.globalDiscovery = new AddressPool();
        SocketAddress[] addrArr = globalDNS.getGlobalDiscoveryAddress();
        if(addrArr == null) return;
        for (int i = 0; i < addrArr.length; i++) {
            globalDiscovery.addAddress(addrArr[i]);
        }
    }
    public DefaultLocalServiceDiscovery(DeprecatedActiveServiceLookup internalLookup) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.globalDiscovery = new AddressPool();
    }
    
    public DefaultLocalServiceDiscovery(DeprecatedActiveServiceLookup internalLookup,GlobalDiscoveryDNS globalDNS) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        initGlobalDiscoveryPool(globalDNS);
    }

    public void setNetwork(DeprecatedNetworkHandler network, SocketAddress localAddress) {
        this.network = network;
        this.localAddress = localAddress;
        System.out.println("local ip: " + ((InetSocketAddress)localAddress).toString());
    }

    private RouteSignal getReturnAddress(DeprecatedServiceConnection destination, DeprecatedServiceConnection from) {
        if(from == null || from.getAddress() == null){
            return RouteSignal.NOTFOUND;
        }
        destination.setAddress(from.getAddress());
        /*if(from.getAddress().equals(localAddress)) {
            return RouteSignal.ENDPOINT;
        }*/
        if(isLocalAddress(from)) {
           // ((DeprecatedDefaultServiceConnection)destination).setUserConnectionId(from.getUserConnectionId());
            return RouteSignal.ENDPOINT;
        }
        
        //isLocalAddress()
        return RouteSignal.RETURNTOORIGIN;
        
    }
    public void stop() {}
    
    private void cacheAddress(String serviceName,SocketAddress targetAddr) {
        AddressPool poolCheck = serviceCache.get(serviceName);
        if(poolCheck != null) {
            poolCheck.addAddress(targetAddr);
            return;
        }
        AddressPool newPool = new AddressPool();
        newPool.addAddress(targetAddr);
        poolCheck = serviceCache.putIfAbsent(serviceName, newPool);
        if(poolCheck != null) { // the above is "atomic" so if a pool already exist now then use it
            poolCheck.addAddress(targetAddr);
        }
    }
    
    private RouteSignal getGlobalAddress(DeprecatedServiceConnection destination, String serviceName) {

        SocketAddress addr = this.globalDiscovery.getAddress();
        if(addr == null){
            return RouteSignal.NOTFOUND;
        }
        DeprecatedDefaultServiceConnection globalDest = new DeprecatedDefaultServiceConnection(UUID.randomUUID());
        globalDest.setPathType(PathType.DISCOVERY);
        DiscoveryProbe discoveryProbe = new DiscoveryProbe(serviceName,null);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
        packet.setProbe(discoveryProbe);
        globalDest.setAddress(addr);
        DeprecatedServiceRequest req = null;
        try {
            req = network.sendWithResponse(packet, globalDest);
        } catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        if(req == null) {
            return RouteSignal.NOTFOUND;
        }

        DiscoveryPacket answers = null;
        try{
            ByteArrayInputStream dataStream = new ByteArrayInputStream(req.getData());
            ObjectInputStream objStream = new ObjectInputStream(dataStream);
            answers = (DiscoveryPacket)objStream.readObject(); //TODO: io checks
        }catch(IOException e){// TODO: Log error
            e.printStackTrace();
        }catch(ClassNotFoundException e){
            e.printStackTrace();
        }

        SocketAddress targetAddr = answers.getProbe().getAddress();
        if(targetAddr == null) {
            return RouteSignal.NOTFOUND;
        }
        
        destination.setAddress(targetAddr);
        cacheAddress(serviceName,targetAddr);
        return RouteSignal.NETWORKDESTINATION;
    }
    
    @Override
    public RouteSignal getDestination(DeprecatedServiceConnection destination, ServiceChain to) {

        if(destination == null) {
            return RouteSignal.NOTFOUND;
        }
        destination.setAddress(localAddress);
        return getDestination(destination,destination,to);
    }
    
    private boolean isLocalAddress(DeprecatedServiceConnection addr) {
        boolean flag = false;
        try {
            flag = ((InetSocketAddress)addr.getAddress()).equals((InetSocketAddress)localAddress);
        }catch(NullPointerException ex) {}        
        return flag;
    }
    
    private boolean isAddressEqual(DeprecatedServiceConnection a, DeprecatedServiceConnection b) {
        boolean flag = false;
        try {
            flag = ((InetSocketAddress)a.getAddress()).equals((InetSocketAddress)a.getAddress());
        }catch(NullPointerException ex) {}        
        return flag;
    }
    
    @Override
    public RouteSignal getDestination(DeprecatedServiceConnection destination, DeprecatedServiceConnection from, ServiceChain to) {

        if(destination == null) {
            return RouteSignal.NOTFOUND;
        }
        String serviceName = null;

        if(to != null) {
            serviceName = to.peekNextServiceName();
        }else if(isLocalAddress(from)) {
            //return RouteSignal.ENDPOINT;
        }

        if(serviceName == null){
            return getReturnAddress(destination, from);
        }

        DeprecatedServiceMetaData serviceInfo = internalLookup.getService(serviceName);
        if(serviceInfo != null){
            return RouteSignal.LOCALDESTINATION;
        }

        AddressPool pool  = serviceCache.get(serviceName);
        if(pool == null) {
            // get global sd
            return getGlobalAddress(destination,serviceName);
        }
        
        destination.setAddress(pool.getAddress());
        return RouteSignal.NETWORKDESTINATION;

    }

    @Override
    public RouteSignal getLocalInterface(DeprecatedServiceConnection from, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private DiscoveryPacket packetUnpack(byte[] data) {

        DiscoveryPacket probe = null;

        try{
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream input = new ObjectInputStream(in);
            probe =  (DiscoveryPacket)input.readObject();

        }catch (IOException ex) {
            Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;

    }

    private void updateAdressTable(DiscoveryProbe probe) {

        SocketAddress check = probe.getAddress();
        String name = probe.getName();

        if(check == null || name == null)
            return;
        try {
            System.out.println("updateAdressTable on: " 
                    + ((InetSocketAddress)localAddress).toString()
                    + "\n\tfor service: " + name
                    + "\n\tat: " + ((InetSocketAddress)check).toString());
        }catch(NullPointerException e) {}
        AddressPool pool = serviceCache.get(probe.getName());

        if(pool != null){
            if(!pool.addAddress(probe.getAddress())){}
                //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);

            return;
        }

        AddressPool servicePool = new AddressPool();
        servicePool.addAddress(probe.getAddress());
        servicePool = serviceCache.putIfAbsent(probe.getName(), servicePool);

        if(servicePool != null){
            if(!servicePool.addAddress(probe.getAddress())){}
                //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);

            return;
        }
    }

    protected void updateHandling(DeprecatedServiceConnection from, DiscoveryPacket packet) {

        DiscoveryPacketType type = packet.getPacketType();
        //to do: switch not functioning properly with enums
       /* 
       System.out.println("DefaultLocalServiceDiscovery: " + type 
                            + " from: " + ((InetSocketAddress)localAddress).toString()
                            + " dest: " + ((InetSocketAddress)packet.getProbe().getAddress()).toString());
       */
       DeprecatedDefaultServiceConnection dest = null;
        switch(type){
        case DISCOVERYREQUEST:
            //updateAdressTable(packet.getProbe());
            //only on global
            System.out.println("DefaultLocalServiceDiscovery updateHandling , bad " + type);
            break;
        case DISCOVERYRESPONSE:
            updateAdressTable(packet.getProbe());
            if(from != null && network != null) {
                //System.out.println("debug updateHandling: from uuid " + from.getUserConnectionId());
                System.out.println("updateAdressTable");
                try{
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    ObjectOutputStream objectStream = new ObjectOutputStream(stream);
                    objectStream.writeObject(packet);
                    network.sendEnd(stream.toByteArray(), from);
                }catch(IOException e){// TODO: Log/Resolve error
                    e.printStackTrace();
                }
            }
            /*if(from != null && network != null){
                dest = new DeprecatedDefaultServiceConnection(from.getUserConnectionId());
                network.sendToService(packet, new DummyServiceChain(),dest );
            }*/
            break;
        case ANNOUNCE:
            //intern handeling method
            break;
        default: //Logger.getLogger(DefaultLocalServiceDiscovery.class.getName()).log(Level.SEVERE, null, null);
            System.out.println("DefaultLocalServiceDiscovery updateHandling recieved, not yet implemented: " + type);
            break;
        }
    }

    @Override
    public void discoveryUpdate(DeprecatedServiceConnection from, byte[] data) {
        DiscoveryPacket packet = packetUnpack(data);
        updateHandling(from, packet);
    }

    @Override
    public boolean announce() {
        SocketAddress addr = this.globalDiscovery.getAddress();
        if(addr == null){
            return false;
        }

        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.ANNOUNCE);
        ArrayList<String> serviceList = new ArrayList<String>();
        for(String nameKey : internalLookup.getKeySet()) {
            serviceList.add(nameKey);
        }
        String[] serviceNameList = serviceList.toArray(new String[serviceList.size()]);
        AnnouncePacket annonce = new AnnouncePacket(localAddress, serviceNameList);
        packet.setAnnonce(annonce);
        System.out.println("Announcing Service on: " + (InetSocketAddress)this.localAddress);
        for (int i = 0; i < serviceNameList.length; i++) {
            System.out.println("\tService: " + serviceNameList[i]);
        }

        DeprecatedDefaultServiceConnection globalDest = new DeprecatedDefaultServiceConnection(UUID.randomUUID());
        globalDest.setPathType(PathType.DISCOVERY);
        globalDest.setAddress(addr);
        try {
            this.network.send(packet, globalDest);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

}
