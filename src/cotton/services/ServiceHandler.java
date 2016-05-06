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

import cotton.internalRouting.InternalRoutingServiceHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.io.Serializable;

public class ServiceHandler implements Runnable{
    private ActiveServiceLookup serviceLookup;
    private InternalRoutingServiceHandler internalRouting;
    private ServiceBuffer workBuffer;
    private ExecutorService threadPool;
    private volatile boolean active = true;

    public ServiceHandler(ActiveServiceLookup serviceLookup, InternalRoutingServiceHandler internalRouting){
        this.internalRouting = internalRouting;
        this.serviceLookup = serviceLookup;
        this.workBuffer = internalRouting.getServiceBuffer();
        threadPool = Executors.newCachedThreadPool();
    }

    public void run(){
        while(active){
            ServicePacket packet = workBuffer.nextPacket();
            if(packet == null){
                try{
                    Thread.sleep(5); //change to exponential fallback strategy.
                }catch(InterruptedException ex){
                }
            }else{
                ServiceDispatcher th = new ServiceDispatcher(packet);
                threadPool.execute(th);
            }
        }
        threadPool.shutdown();
    }

    public void stop(){
        this.active = false;
    }

    private class ServiceDispatcher implements Runnable{
        private ServiceFactory serviceFactory;
        private boolean succesfulInit = true;
        private ServicePacket servicePacket;
        private String serviceName;
        public ServiceDispatcher(ServicePacket servicePacket){
            this.servicePacket = servicePacket;
            this.serviceName = this.servicePacket.getTo().getNextServiceName();

            if(this.serviceName == null){
                succesfulInit = false;
                return;
            }

            ServiceMetaData serviceMetaData = serviceLookup.getService(serviceName);
            if(serviceMetaData == null){
                succesfulInit = false;
                return;
            }
            int currentServiceCount = serviceMetaData.incrementThreadCount();
            if(currentServiceCount > serviceMetaData.getMaxCapacity()){
                serviceMetaData.decrementThreadCount();
                succesfulInit = false;
                return;
            }

            this.serviceFactory = serviceMetaData.getServiceFactory();

        }
        @Override
        public void run(){
            if(succesfulInit == false)
                return;
            Service service = serviceFactory.newService();
            try{

                byte[] result = service.execute(null, servicePacket.getOrigin(), servicePacket.getData(),servicePacket.getTo());
                
                internalRouting.forwardResult(servicePacket.getOrigin(), servicePacket.getTo(), result);
                serviceLookup.getService(serviceName).decrementThreadCount(); 
                internalRouting.notifyRequestQueue(serviceName);
                
            }catch(Exception e){
                serviceLookup.getService(serviceName).decrementThreadCount(); 
                e.printStackTrace();
            }
        }
    }
}
