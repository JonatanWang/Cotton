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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * The <code>ServiceMetaData</code> consists of information about the service. 
 * The <code>ServiceHandler</code> needs to manually update the thread count for
 * the <code>currentThreadCount</code> to work.
 *
 * @author Tony
 * @author Magnus
 * @see ServiceHandler
 */
public class ServiceMetaData {
    private ServiceFactory serviceFactory;
    private int maxCapacity;
    private AtomicInteger currentThreadCount;

    /**
     * Constructs a object with information about the service. The thread count
     * will start at zero.
     * 
     * @param serviceFactory the <code>ServiceFactory</code> for the service.
     * @param maxCapacity defines the maximum amount of simultaneously services.
     */
    public ServiceMetaData(ServiceFactory serviceFactory, int maxCapacity) {
            this.maxCapacity = maxCapacity;
            this.serviceFactory = serviceFactory;
            currentThreadCount = new AtomicInteger();
    }

    /**
     * Increments the number keeping track of running instances. This function
     * should be executed when a new thread is launched.
     * 
     * @return the number of instances running.
     */
    public int incrementThreadCount(){
            return currentThreadCount.incrementAndGet();
    }

    /**
     * Decrements the number keeping track of running instances. This function
     * should be executed when a thread is turned off.
     * 
     * @return the number of instances running.
     */
    public int decrementThreadCount(){
            return currentThreadCount.decrementAndGet();
    }

    /**
     * Returns the maximum amount of instances that are allowed to run 
     * simultaneously.
     * 
     * @return maximum capacity of the service.
     */
    public int getMaxCapacity() {
            return maxCapacity;
    }

    /**
     * Returns the current amount of instances running in the thread pool.
     * 
     * @return current amount of instances.
     */
    public int getCurrentThreadCount(){
            return currentThreadCount.get();
    }

    /**
     * Returns the <code>ServiceFactory</code> connected to the service.
     * 
     * @return the <code>ServiceFactory</code> connected to the service.
     */
    public ServiceFactory getServiceFactory(){
            return serviceFactory;
    }
}
