/*

Copyright (c) 2016, Gunnlaugur Juliusson, Jonathan Kåhre, Magnus Lundmark,
Mats Levin, Tony Tran
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Cotton Production Team nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */


package cotton;

import cotton.network.DefaultNetworkHandler;
import cotton.network.SocketSelectionNetworkHandler;
import cotton.network.TokenManager;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.network.NetworkHandler;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceHandler;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.services.ServiceLookup;
import cotton.internalrouting.DefaultInternalRouting;
import cotton.internalrouting.InternalRoutingClient;
import java.util.Random;
import cotton.requestqueue.RequestQueueManager;
import cotton.storagecomponents.MongoDBConnector;
import cotton.systemsupport.Console;
import cotton.configuration.Configurator;
import cotton.configuration.ServiceConfigurator;
import java.net.UnknownHostException;

/**
 *
 * @author Jonathan
 * @author Magnus
 * @author Gunnlaugur
 * @author Tony
 * @author Mats
 */
public class Cotton {
    private ActiveServiceLookup lookup;
    private NetworkHandler network;
    private ServiceHandler services;
    private ServiceDiscovery discovery;
    private DefaultInternalRouting internalRouting;
    private Console console = new Console();
    private TokenManager tm;
    private RequestQueueManager rqm = null;

    public Cotton(Configurator config) throws java.net.UnknownHostException,
                                              java.net.MalformedURLException,
                                              ClassNotFoundException,
                                              InstantiationException,
                                              IllegalAccessException{

        if(config.hasDatabase())
            dataBaseWrapperStart(config);
        //initNetwork(new SocketSelectionNetworkHandler(config.getNetworkConfigurator()));
        initNetwork(new DefaultNetworkHandler(config.getNetworkConfigurator()));
        initDiscovery(config);
        initLookup(config.getServiceConfigurator());
        initRouting();
        initServiceHandler();
        if(config.isQueue())
            queueStart(config);
    }

    /**

     * This starts a new cotton node for your cloud application
     * @param globalServiceDiscovery if this should be a globalDiscovery node that other ask for directions from
     * @throws java.net.UnknownHostException can resolve localhost address
     */
    public Cotton(boolean globalServiceDiscovery) throws java.net.UnknownHostException {
        initNetwork(null);
        initDiscovery(globalServiceDiscovery,null);
        initLookup();
        initRouting();
        initServiceHandler();
    }

    /**
     * This starts a new cotton node for your cloud application
     * @param globalServiceDiscovery if this should be a globalDiscovery node that other ask for directions from
     * @param globalDiscoveryDNS tells this node where to find globalServiceDiscoverys to reach the cloud
     * @throws java.net.UnknownHostException
     */
    public Cotton(boolean globalServiceDiscovery,GlobalDiscoveryDNS globalDiscoveryDNS) throws java.net.UnknownHostException {
        initNetwork(null);
        initDiscovery(globalServiceDiscovery,globalDiscoveryDNS);
        initLookup();
        initRouting();
        initServiceHandler();
    }

    /**
     * This starts a new cotton node for your cloud application
     * @param globalServiceDiscovery if this should be a globalDiscovery node that other ask for directions from
     * @param localPort the port this should listen on
     * @param globalDiscoveryDNS tells this node where to find globalServiceDiscoverys to reach the cloud
     * @throws java.net.UnknownHostException
     */
    public Cotton(boolean globalServiceDiscovery,int localPort, GlobalDiscoveryDNS globalDiscoveryDNS) throws java.net.UnknownHostException {
        initNetwork(new DefaultNetworkHandler(localPort));
        //initNetwork(new SocketSelectionNetworkHandler(localPort));
        initDiscovery(globalServiceDiscovery,globalDiscoveryDNS);
        initLookup();
        initRouting();
        initServiceHandler();
    }
    
    /**
     * This starts a new cotton node for your cloud application
     * @param globalServiceDiscovery if this should be a globalDiscovery node that other ask for directions from
     * @param portNumber what port this node should listen on
     * @throws java.net.UnknownHostException
     */
    public Cotton (boolean globalServiceDiscovery, int portNumber) throws java.net.UnknownHostException {
        //initNetwork(new SocketSelectionNetworkHandler(portNumber));
        initNetwork(new DefaultNetworkHandler(portNumber));
        initDiscovery(globalServiceDiscovery,null);
        initLookup();
        initRouting();
        initServiceHandler();
    }

    /**
     *
     * @param globalServiceDiscovery
     * @param net
     * @throws java.net.UnknownHostException
     */
    public Cotton (boolean globalServiceDiscovery, NetworkHandler net) throws java.net.UnknownHostException {
        initNetwork(net);
        initDiscovery(globalServiceDiscovery,null);
        initLookup();
        initRouting();
        initServiceHandler();
    }

    /**
     *
     * @param globalServiceDiscovery
     * @param globalDiscoveryDNS
     * @param net
     * @throws java.net.UnknownHostException
     */
    public Cotton(boolean globalServiceDiscovery,GlobalDiscoveryDNS globalDiscoveryDNS, NetworkHandler net) throws java.net.UnknownHostException {
        initNetwork(net);
        initDiscovery(globalServiceDiscovery,globalDiscoveryDNS);
        initLookup();
        initRouting();
        initServiceHandler();
    }

    /**
     *
     */
    public void start(){
        new Thread(network).start();
        internalRouting.setCommandControl(console);
        internalRouting.start();
        new Thread(services).start();
        if(!discovery.announce()){
            System.out.println("Announce failed");
        }
        this.console.addSubSystem(discovery);
        this.console.addSubSystem(services);
        this.console.addSubSystem(internalRouting);
    }

    /**
     *
     * @return
     */
    public Console getConsole() {
        return console;
    }

    /**
     *
     */
    public void shutdown() {
        services.stop();
        discovery.stop();
        network.stop();
        internalRouting.stop();
    }

    /**
     *
     * @return
     */
    public ActiveServiceLookup getServiceRegistation() {
        return lookup;
    }

    /**
     *
     * @return
     */
    public NetworkHandler getNetwork() {
        return network;
    }

    /**
     *
     * @return
     */
    public InternalRoutingClient getClient(){
        return internalRouting;
    }
    
    public RequestQueueManager getRequestQueueManager() {
        if(rqm == null)
            throw new NullPointerException("RQM: Null");
        
        return rqm;
    }

    /**
     *
     * @param requestQueueManager
     */
    public void setRequestQueueManager(RequestQueueManager requestQueueManager){
        this.internalRouting.setRequestQueueManager(requestQueueManager);
        this.console.addSubSystem(requestQueueManager);
    }

    /**
     * Creates the connector to the database so that it's possible to send and receive data to the database.
     *
     * @param con configuration file containing information regarding database connection.
     */
    public void dataBaseWrapperStart (Configurator con){
        MongoDBConnector db = new MongoDBConnector(con.getDatabaseConfigurator());
        tm  = new TokenManager();
        db.setTokenManager(tm);
    }
    
    public void databaseWrapperStart() {
        MongoDBConnector db = new MongoDBConnector();
        tm  = new TokenManager();
        db.setTokenManager(tm);
    }
    
    private void queueStart(Configurator conf) {
        rqm = new RequestQueueManager(conf.getQueueConfigurator());
        
        this.internalRouting.setRequestQueueManager(rqm);
        this.console.addSubSystem(rqm);
    }

    private void initNetwork(NetworkHandler net) throws UnknownHostException {
        if(net == null) {
            Random rnd = new Random();
            net = new DefaultNetworkHandler(rnd.nextInt(20000) + 3000);
            //net = new SocketSelectionNetworkHandler(rnd.nextInt(20000)+3000);
        }
        this.network = net;
    }

    private void initDiscovery(boolean globalServiceDiscovery,GlobalDiscoveryDNS globalDiscoveryDNS) {
        if(globalDiscoveryDNS == null) {
            globalDiscoveryDNS = new GlobalDiscoveryDNS();
        }
        if (globalServiceDiscovery) {
            this.discovery = new GlobalServiceDiscovery(globalDiscoveryDNS);
        } else {
            this.discovery = new LocalServiceDiscovery(globalDiscoveryDNS);
        }
    }
    
    private void initDiscovery(Configurator conf) {
        if(conf == null) {
            throw new NullPointerException("Global discovery conf settings missing");
        }
        if (conf.isGlobal()) {
            this.discovery = new GlobalServiceDiscovery(true, conf);
        } else {
            this.discovery = new LocalServiceDiscovery(conf);
        }
    }

    private void initLookup() {
        this.lookup = new ServiceLookup();
        this.discovery.setLocalServiceTable(lookup);
    }

    private void initLookup(ServiceConfigurator config) throws java.net.MalformedURLException,
                                                               ClassNotFoundException,
                                                               InstantiationException,
                                                               IllegalAccessException{
        this.lookup = new ServiceLookup(config);
        this.discovery.setLocalServiceTable(lookup);
    }

    private void initRouting() {
        this.internalRouting = new DefaultInternalRouting(this.network, this.discovery);
    }

    private void initServiceHandler() {
        this.services = new ServiceHandler(lookup, internalRouting);
    }

    public static void main(String[] args) {
        Cotton c = null;
        try{
            c = new Cotton(true,3333);
            c.start();
        }catch(java.net.UnknownHostException e){// TODO: Rethink this
            System.out.println("Init network error, exiting");
            return;
        }finally{
            c.shutdown();
        }
    }
}
