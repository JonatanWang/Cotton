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


package cotton.services;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.Set;
import java.util.Map;
import cotton.configuration.ServiceConfigurator;
import java.net.URLClassLoader;
import java.io.File;
import java.net.URL;

/**
*
* @author Tony
* @author Jonathan Kåhre
*/
public class ServiceLookup implements ActiveServiceLookup{

  	private ConcurrentHashMap<String,ServiceMetaData> hashMap;

  	public ServiceLookup(){
        this.hashMap = new ConcurrentHashMap<>();
  	}

    public ServiceLookup(ServiceConfigurator config) throws java.net.MalformedURLException,
                                                            ClassNotFoundException,
                                                            InstantiationException,
                                                            IllegalAccessException{

        this.hashMap = new ConcurrentHashMap<>();

        if(config.hasServices()){
            String workingDirectory = System.getProperty("user.dir");
            File f = new File(workingDirectory+"/services/");
            URL url = f.toURL();
            URL[] urls = new URL[]{url};
            ClassLoader classloader = new URLClassLoader(urls);

            for(ServiceConfigurator.ServiceData serviceInfo: config.getServices()){
                Class s = classloader.loadClass(serviceInfo.getName());
                Service service = (Service)s.newInstance();
                registerService(serviceInfo.getName(), service.loadFactory(), serviceInfo.getLimit());
            }
        }
    }

    /**
     * Registers a service to the lookup table. To register a service the user
     * has to define the service name, the factory used to make instances of the service
     * and the maximum capacity of the service instances.
     *
     * @param serviceName defines what the service is called.
     * @param serviceFactory the factory used to make instances of the service.
     * @param maxCapacity defines how many instances of the service are allowed.
     * @return whether the registration was successful or not.
     */
    public boolean registerService(String serviceName, ServiceFactory serviceFactory, int maxCapacity){
        ServiceMetaData metaData = new ServiceMetaData(serviceFactory, maxCapacity);
        if(hashMap.putIfAbsent(serviceName, metaData) == null) {
            return true;    // no mapping for this key
        }
        return false;

    }

    /**
     * Returns the <code>ServiceMetaData</code> for a specified service.
     *
     * @param serviceName the specified service.
     * @return the meta data of the service.
     */
  	public ServiceMetaData getService(String serviceName){
  		return hashMap.get(serviceName);
  	}

    /**
     * Returns an <code>String Enumeration</code> of the keys in the lookup table.
     * The order of the keys will be kept from the lookup table.
     *
     * @return the keys in the lookup table.
     */
  	public Enumeration<String> getServiceEnumeration(){
  		return hashMap.keys();
  	}

    /**
     * Returns the key set in the hash map table. The order of the keys will be
     * unpredictable.
     *
     * @return the key set of the hash map.
     */
  	public ConcurrentHashMap.KeySetView<String, ServiceMetaData> getKeySet(){
  		return hashMap.keySet();
  	}
    /**
     * Returns a set of key value pairs. The order of the key value pairs will be
     * unpredictable
     * @return the entrys of the hash map.
     */
    public Set<Map.Entry<String,ServiceMetaData>> getEntrySet(){
        return hashMap.entrySet();
    }
    
    /**
     * Removes a service entry from the lookup table.
     *
     * @param service the service name.
     * @return the meta data about the removed service.
     */
  	public ServiceMetaData removeServiceEntry(String service){
  		return hashMap.remove(service);
  	}
}
