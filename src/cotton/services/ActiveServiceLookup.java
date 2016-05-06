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

/**
 * The <code>ActiveServiceLookup</code> acts as a lookup table for services. The
 * class implementing the interface is intended to be used by the 
 * <code>ServiceHandler</code> to register, get services as well as remove services 
 * from the lookup table.
 * 
 * @author Tony
 * @author Magnus
 * @see ServiceHandler
 **/
public interface ActiveServiceLookup {

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
    public boolean registerService(String serviceName, ServiceFactory serviceFactory,int maxCapacity);

    /**
     * Returns the <code>ServiceMetaData</code> for a specified service.
     * 
     * @param serviceName the specified service.
     * @return the meta data of the service.
     */
    public ServiceMetaData getService(String serviceName);
    
    /**
     * Returns an <code>String Enumeration</code> of the keys in the lookup table.
     * The order of the keys will be kept from the lookup table.
     * 
     * @return the keys in the lookup table.
     */
    public Enumeration<String> getServiceEnumeration();
    
    /**
     * Returns the key set in the hash map table. The order of the keys will be
     * unpredictable.
     * 
     * @return the key set of the hash map.
     */
    public ConcurrentHashMap.KeySetView<String, ServiceMetaData> getKeySet();

    /**
     * Removes a service entry from the lookup table.
     * 
     * @param service the service name.
     * @return the meta data about the removed service.
     */
    public ServiceMetaData removeServiceEntry(String service);
}
