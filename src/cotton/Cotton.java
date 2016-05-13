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

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import cotton.example.FileWriterService;
import cotton.example.ImageManipulationPacket;
import cotton.example.ImageManipulationService;
import cotton.network.ServiceChain;
import cotton.servicediscovery.GlobalDiscoveryDNS;
import cotton.network.DummyServiceChain;
import java.util.concurrent.ThreadLocalRandom;
import cotton.network.NetworkHandler;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceHandler;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.servicediscovery.LocalServiceDiscovery;
import cotton.services.ServiceLookup;
import cotton.services.ServiceFactory;
import cotton.internalRouting.DefaultInternalRouting;
import cotton.internalRouting.InternalRoutingClient;
import cotton.network.DefaultNetworkHandler;
import java.util.Random;
import cotton.requestqueue.RequestQueueManager;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatisticsProvider;

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

    public Cotton(boolean globalServiceDiscovery) throws java.net.UnknownHostException {
        Random rnd = new Random();
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        NetworkHandler net = null;
        if (globalServiceDiscovery) {
            net = new DefaultNetworkHandler(rnd.nextInt(20000) + 3000);
            discovery = new GlobalServiceDiscovery(globalDiscoveryDNS);

        } else {
            net = new DefaultNetworkHandler(rnd.nextInt(20000) + 3000);
            discovery = new LocalServiceDiscovery(globalDiscoveryDNS);

        }
        lookup = new ServiceLookup();
        discovery.setLocalServiceTable(lookup);
        this.internalRouting = new DefaultInternalRouting(net, discovery);
        this.services = new ServiceHandler(lookup, internalRouting);
        //clientNetwork = net;
        //services = new DeprecatedServiceHandler(lookup, network);
        //TODO swap for current versions
        this.network = net;
    }

    public Cotton(boolean globalServiceDiscovery, GlobalDiscoveryDNS globalDiscoveryDNS) throws java.net.UnknownHostException {
        Random rnd = new Random();
        if(globalDiscoveryDNS == null) {
            globalDiscoveryDNS = new GlobalDiscoveryDNS();
        }
        NetworkHandler net = null;
        if (globalServiceDiscovery) {
            net = new DefaultNetworkHandler(rnd.nextInt(20000) + 3000);
            discovery = new GlobalServiceDiscovery(globalDiscoveryDNS);

        } else {
            net = new DefaultNetworkHandler(rnd.nextInt(20000) + 3000);
            discovery = new LocalServiceDiscovery(globalDiscoveryDNS);

        }
        lookup = new ServiceLookup();
        discovery.setLocalServiceTable(lookup);
        this.internalRouting = new DefaultInternalRouting(net, discovery);
        this.services = new ServiceHandler(lookup, internalRouting);
        //clientNetwork = net;
        //services = new DeprecatedServiceHandler(lookup, network);
        //TODO swap for current versions
        this.network = net;
    }

    public Cotton (boolean globalServiceDiscovery, int portNumber) throws java.net.UnknownHostException {
        //TODO swap for current versions
        GlobalDiscoveryDNS globalDiscoveryDNS = new GlobalDiscoveryDNS();
        NetworkHandler net = null;
        if(globalServiceDiscovery) {
            net = new DefaultNetworkHandler(portNumber);
            discovery = new GlobalServiceDiscovery(globalDiscoveryDNS);
        }else {
            net = new DefaultNetworkHandler(portNumber);
            discovery = new LocalServiceDiscovery(globalDiscoveryDNS);
        }
        lookup = new ServiceLookup();
        discovery.setLocalServiceTable(lookup);
        this.internalRouting = new DefaultInternalRouting(net,discovery);
        this.services = new ServiceHandler(lookup,internalRouting);
        this.network = net;
    }

    public void start(){
        new Thread(network).start();
        internalRouting.setCommandControl(console);
        internalRouting.start();
        new Thread(services).start();
        discovery.announce();
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
