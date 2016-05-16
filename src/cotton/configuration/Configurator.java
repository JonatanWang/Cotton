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

package cotton.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

/**
 * Configurator for the cotton cloud server
 *
 * @author Jonathan Kåhre
 */
public class Configurator {
    Properties prop = new Properties();

    public static void main(String[] args) {
        Configurator config = new Configurator();

        try{
            config.loadConfigFromFile("config.cfg");
            //config.loadConfigFromFile("configurationtemplate.cfg");
        }catch(Exception e){
            e.printStackTrace();
        }
        try{
            System.out.println("DB: "+config.getDatabaseConfigurator());
            System.out.println("Network: "+config.getNetworkConfigurator());
            System.out.println("Services: "+config.getServiceConfigurator());
            System.out.println("Queue: "+config.getQueueConfigurator());
            cotton.network.DefaultNetworkHandler handler = new cotton.network.DefaultNetworkHandler(config.getNetworkConfigurator());
            cotton.services.ServiceLookup lookup = new cotton.services.ServiceLookup(config.getServiceConfigurator());
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    
    public Configurator(){
        
    }
    
    public Configurator(String filename) throws IOException{
        loadConfigFromFile(filename);
    }

    /**
     * Returns whether this node is configured to be a global service discovery or not.
     *
     * @return whether this node is configured to be a global service discovery or not
     */
    public boolean isGlobal(){
        if(prop == null)
            throw new IllegalStateException("Configurator empty / not initialised");
        String serviceDiscovery;
        if((serviceDiscovery = prop.getProperty("serviceDiscovery")) == null || serviceDiscovery.equalsIgnoreCase("local"))
            return false;
        else
            return true;
    }
    
    public boolean hasDatabase(){
        if(prop == null)
            throw new IllegalStateException("Configurator empty / not initialised");
        String database;
        if((database = prop.getProperty("dbEnabled")) == null || database.equalsIgnoreCase("false"))
            return false;
        else
            return true;
    }

    /**
     * Loads configuration file from filename.
     *
     * @param filename The name of the file to load
     */
    public void loadConfigFromFile(String filename) throws IOException{
        if(prop == null)
            prop = new Properties();
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        if(in == null)
            System.out.println("File not found in configurator");
        
        prop.load(in);
    }
    
    public void loadDefaults(){
        if(prop == null)
            prop = new Properties();
        prop.setProperty("backend", "mongodb");
        prop.setProperty("dbname", "cotton");
        prop.setProperty("dbAddress", "localhost");
    }

    /**
     * Builds and returns a <code>NetworkConfigurator</code> from loaded specification.
     *
     * @return The newly generated <code>NetworkConfigurator</code>
     */
    public NetworkConfigurator getNetworkConfigurator() throws java.net.UnknownHostException{
        if(prop == null)
            throw new IllegalStateException("Configurator empty / not initialised.");
        NetworkConfigurator.Builder builder = new NetworkConfigurator.Builder();
        String addressPort;
        InetSocketAddress address = null;
        if((addressPort = prop.getProperty("networkAddress")) == null || addressPort.equalsIgnoreCase("localhost"))
            address = new InetSocketAddress(InetAddress.getByName(null), 3333);
        else if(addressPort != null && addressPort.equalsIgnoreCase("random"))
            address = new InetSocketAddress(InetAddress.getByName(null), new Random().nextInt(40000)+5000);

        if(address == null){
            String[] splitAddress = addressPort.split(":");

            if(splitAddress.length > 1){
                int port;
                if(!splitAddress[1].equalsIgnoreCase("rand"))
                    port = Integer.parseInt(splitAddress[1]);
                else
                    port = new Random().nextInt(40000)+5000;

                if(splitAddress[0].equalsIgnoreCase("localhost"))
                    address = new InetSocketAddress(InetAddress.getByName(null), port);
                else
                    address = new InetSocketAddress(splitAddress[0], port);
            }else if(splitAddress.length > 0){
                if(splitAddress[0].equalsIgnoreCase("localhost"))
                    address = new InetSocketAddress(InetAddress.getByName(null), 3333);
                else
                    address = new InetSocketAddress(splitAddress[0], 3333);
            }else
                address = new InetSocketAddress(InetAddress.getByName(null), 3333);
        }
        builder.setAddress(address);

        String encryptionString;
        boolean encryption = false;
        if((encryptionString = prop.getProperty("encryption")) != null && encryptionString.equalsIgnoreCase("enabled")){
            encryption = true;
        }
        builder.setEncryption(encryption);

        if(encryption){
            String keystore;
            String password;

            if((keystore = prop.getProperty("keystore")) == null)
                throw new MalformedConfigurationException("Encryption enabled but no keystore specified.");
            if((password = prop.getProperty("password")) == null)
                throw new MalformedConfigurationException("Encryption enabled but no keystore password specified.");

            builder.setKeystore(keystore);
            builder.setPassword(password);
        }

        return builder.build();
    }

    /**
     * Builds and returns a <code>DatabaseConfigurator</code> from loaded specification.
     *
     * @return The newly generated <code>DatabaseConfigurator</code>
     */
    public DatabaseConfigurator getDatabaseConfigurator(){
        if(prop == null)
            throw new IllegalStateException("Configurator empty / not initialised.");
        DatabaseConfigurator.Builder builder = new DatabaseConfigurator.Builder();

        String backend;
        if((backend = prop.getProperty("backend")) == null)
            backend = "MongoDB";
        builder.setBackend(backend.toLowerCase());

        String addressPort;
        if((addressPort = prop.getProperty("dbAddress")) == null)
            throw new MalformedConfigurationException("No database address specified.");
        String[] splitAddress = addressPort.split(":");
        if(splitAddress.length > 1){
            builder.setAddress(splitAddress[0].toLowerCase());
            builder.setPort(Integer.parseInt(splitAddress[1]));
        }else if(splitAddress.length > 0)
            throw new MalformedConfigurationException("No database port specified.");

        String dbname;
        if((dbname = prop.getProperty("dbname")) == null)
            throw new MalformedConfigurationException("No database name specified.");
        builder.setDatabasename(dbname.toLowerCase());

        return builder.build();
    }

    /**
     * Builds and returns a <code>QueueConfigurator</code> from loaded specification.
     *
     * @return The newly generated <code>QueueConfigurator</code>
     */
    public QueueConfigurator getQueueConfigurator(){
        if(prop == null)
            throw new IllegalStateException("Configurator empty / not initialised.");
        QueueConfigurator.Builder builder = new QueueConfigurator.Builder();

        String disabledServices;
        if((disabledServices = prop.getProperty("disabledServices")) != null)
            for (String serviceName : disabledServices.split(","))
                builder.addDisabledService(serviceName);

        String queueLimit;
        if((queueLimit = prop.getProperty("queueLimit")) == null)
            queueLimit = "5";
        builder.setQueueLimit(Integer.parseInt(queueLimit));

        return builder.build();
    }

    /**
     * Builds and returns a <code>ServiceConfigurator</code> from loaded specification.
     *
     * @return The newly generated <code>ServiceConfigurator</code>
     */
    public ServiceConfigurator getServiceConfigurator(){
        if(prop == null)
            throw new IllegalStateException("Configurator empty / not initialised.");
        ServiceConfigurator.Builder builder = new ServiceConfigurator.Builder();

        String services;
        if((services = prop.getProperty("services")) != null)
            for (String service : services.split(",")) {
                String[] serviceData = service.split(":");
                int serviceLimit = 0;
                if(serviceData.length > 1){
                    serviceLimit = Integer.parseInt(serviceData[1]);
                }else if(serviceData.length > 0){
                    serviceLimit = 5;
                }
                builder.addService(serviceData[0], serviceLimit);
            }

        String concurrentServiceLimit;
        if((concurrentServiceLimit = prop.getProperty("concurrentServiceLimit")) == null)
            concurrentServiceLimit = "15";
        builder.setThreadLimit(Integer.parseInt(concurrentServiceLimit));

        return builder.build();
    }

    public SocketAddress[] getDiscoverySocketAddresses() {
        ArrayList<SocketAddress> addresses = new ArrayList<>();
        String addressString = null;
        if((addressString = prop.getProperty("discoveryAddresses")) != null)
            for (String address : addressString.split(",")) {
                String[] addressData = address.split(":");
                int port = 0;
                if(addressData.length > 1){
                    port = Integer.parseInt(addressData[1]);
                }else if(addressData.length > 0){
                    port = 5;
                }
                SocketAddress sa = new InetSocketAddress(addressData[0], port);
                addresses.add(sa);
            }
        SocketAddress[] result = new SocketAddress[addresses.size()];
        
        return addresses.toArray(result);
    }
}
