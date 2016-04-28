
package cotton.servicediscovery;

import cotton.network.DefaultServiceConnection;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import cotton.network.ServiceRequest;
import cotton.services.ActiveServiceLookup;
import cotton.network.ServiceConnection;
import java.io.InputStream;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import cotton.services.ServiceMetaData;
import java.util.UUID;
import cotton.network.PathType;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Enumeration;
import cotton.network.DeprecatedNetworkHandler;

/**
 *
 * @author Magnus, Mats
 */
public class DefaultLocalServiceDiscovery implements ServiceDiscovery {
    private ActiveServiceLookup internalLookup;
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
    public DefaultLocalServiceDiscovery(ActiveServiceLookup internalLookup) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        this.globalDiscovery = new AddressPool();
    }
    
    public DefaultLocalServiceDiscovery(ActiveServiceLookup internalLookup,GlobalDiscoveryDNS globalDNS) {
        this.internalLookup = internalLookup;
        this.serviceCache = new ConcurrentHashMap<String, AddressPool>();
        initGlobalDiscoveryPool(globalDNS);
    }

    public void setNetwork(DeprecatedNetworkHandler network, SocketAddress localAddress) {
        this.network = network;
        this.localAddress = localAddress;
        System.out.println("local ip: " + ((InetSocketAddress)localAddress).toString());
    }

    private RouteSignal getReturnAddress(ServiceConnection destination, ServiceConnection from) {
        if(from == null || from.getAddress() == null){
            return RouteSignal.NOTFOUND;
        }
        destination.setAddress(from.getAddress());
        /*if(from.getAddress().equals(localAddress)) {
            return RouteSignal.ENDPOINT;
        }*/
        if(isLocalAddress(from)) {
           // ((DefaultServiceConnection)destination).setUserConnectionId(from.getUserConnectionId());
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
    
    private RouteSignal getGlobalAddress(ServiceConnection destination, String serviceName) {

        SocketAddress addr = this.globalDiscovery.getAddress();
        if(addr == null){
            return RouteSignal.NOTFOUND;
        }
        DefaultServiceConnection globalDest = new DefaultServiceConnection(UUID.randomUUID());
        globalDest.setPathType(PathType.DISCOVERY);
        DiscoveryProbe discoveryProbe = new DiscoveryProbe(serviceName,null);
        DiscoveryPacket packet = new DiscoveryPacket(DiscoveryPacketType.DISCOVERYREQUEST);
        packet.setProbe(discoveryProbe);
        globalDest.setAddress(addr);
        ServiceRequest req = null;
        try {
            req = network.sendWithResponse(packet, globalDest);
        } catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        if(req == null) {
            return RouteSignal.NOTFOUND;
        }

        DiscoveryPacket answers = (DiscoveryPacket)req.getData(); //TODO: io checks
        
        SocketAddress targetAddr = answers.getProbe().getAddress();
        if(targetAddr == null) {
            return RouteSignal.NOTFOUND;
        }
        
        destination.setAddress(targetAddr);
        cacheAddress(serviceName,targetAddr);
        return RouteSignal.NETWORKDESTINATION;
    }
    
    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceChain to) {

        if(destination == null) {
            return RouteSignal.NOTFOUND;
        }
        destination.setAddress(localAddress);
        return getDestination(destination,destination,to);
    }
    
    private boolean isLocalAddress(ServiceConnection addr) {
        boolean flag = false;
        try {
            flag = ((InetSocketAddress)addr.getAddress()).equals((InetSocketAddress)localAddress);
        }catch(NullPointerException ex) {}        
        return flag;
    }
    
    private boolean isAddressEqual(ServiceConnection a, ServiceConnection b) {
        boolean flag = false;
        try {
            flag = ((InetSocketAddress)a.getAddress()).equals((InetSocketAddress)a.getAddress());
        }catch(NullPointerException ex) {}        
        return flag;
    }
    
    @Override
    public RouteSignal getDestination(ServiceConnection destination, ServiceConnection from, ServiceChain to) {

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

        ServiceMetaData serviceInfo = internalLookup.getService(serviceName);
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
    public RouteSignal getLocalInterface(ServiceConnection from, ServiceChain to) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private DiscoveryPacket packetUnpack(InputStream data) {

        DiscoveryPacket probe = null;

        try{
            ObjectInputStream input = new ObjectInputStream(data);
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

    protected void updateHandling(ServiceConnection from, DiscoveryPacket packet) {

        DiscoveryPacketType type = packet.getPacketType();
        //to do: switch not functioning properly with enums
       /* 
       System.out.println("DefaultLocalServiceDiscovery: " + type 
                            + " from: " + ((InetSocketAddress)localAddress).toString()
                            + " dest: " + ((InetSocketAddress)packet.getProbe().getAddress()).toString());
       */
       DefaultServiceConnection dest = null;
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
                System.out.println("updateAdressTable" );
                network.sendEnd(packet, from);
            }
            /*if(from != null && network != null){
                dest = new DefaultServiceConnection(from.getUserConnectionId());
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
    public void discoveryUpdate(ServiceConnection from, InputStream data) {
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
        AnnoncePacket annonce = new AnnoncePacket(localAddress, serviceNameList);
        packet.setAnnonce(annonce);
        System.out.println("Announcing Service on: " + (InetSocketAddress)this.localAddress);
        for (int i = 0; i < serviceNameList.length; i++) {
            System.out.println("\tService: " + serviceNameList[i]);
        }
        
        DefaultServiceConnection globalDest = new DefaultServiceConnection(UUID.randomUUID());
        globalDest.setPathType(PathType.DISCOVERY);
        globalDest.setAddress(addr);
        this.network.send(packet, globalDest);
        return true;
    }

}
