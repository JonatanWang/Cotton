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
import cotton.network.DefaultNetworkHandler;
import java.util.Random;
import cotton.requestqueue.RequestQueueManager;
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
 */
public class Cotton {
    private ActiveServiceLookup lookup;
    private NetworkHandler network;
    private ServiceHandler services;
    private ServiceDiscovery discovery;
    private DefaultInternalRouting internalRouting;
    private Console console = new Console();

    public Cotton(Configurator config) throws java.net.UnknownHostException,
                                              java.net.MalformedURLException,
                                              ClassNotFoundException,
                                              InstantiationException,
                                              IllegalAccessException{

        initNetwork(new DefaultNetworkHandler(config.getNetworkConfigurator()));
        initDiscovery(config.isGlobal(), null);
        initLookup(config.getServiceConfigurator());
        initRouting();
        initServiceHandler();
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
     * @param portNumber what port this node should listen on
     * @throws java.net.UnknownHostException
     */
    public Cotton (boolean globalServiceDiscovery, int portNumber) throws java.net.UnknownHostException {
        initNetwork(new DefaultNetworkHandler(portNumber));
        initDiscovery(globalServiceDiscovery,null);
        initLookup();
        initRouting();
        initServiceHandler();
    }
    public Cotton (boolean globalServiceDiscovery, NetworkHandler net) throws java.net.UnknownHostException {
        initNetwork(net);
        initDiscovery(globalServiceDiscovery,null);
        initLookup();
        initRouting();
        initServiceHandler();
    }

    public Cotton(boolean globalServiceDiscovery,GlobalDiscoveryDNS globalDiscoveryDNS, NetworkHandler net) throws java.net.UnknownHostException {
        initNetwork(net);
        initDiscovery(globalServiceDiscovery,globalDiscoveryDNS);
        initLookup();
        initRouting();
        initServiceHandler();
    }

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

    public Console getConsole() {
        return console;
    }

    public void shutdown() {
        services.stop();
        discovery.stop();
        network.stop();
        internalRouting.stop();

    }

    public ActiveServiceLookup getServiceRegistation() {
        return lookup;
    }

    public NetworkHandler getNetwork() {
        return network;
    }

    public InternalRoutingClient getClient(){
        return internalRouting;
    }

    public void setRequestQueueManager(RequestQueueManager requestQueueManager){
        this.internalRouting.setRequestQueueManager(requestQueueManager);
        this.console.addSubSystem(requestQueueManager);
    }

    private void initNetwork(NetworkHandler net) throws UnknownHostException {
        if(net == null) {
            Random rnd = new Random();
            net = new DefaultNetworkHandler(rnd.nextInt(20000) + 3000);
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
