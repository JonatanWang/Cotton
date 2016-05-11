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
package cotton.requestqueue;

import cotton.internalRouting.InternalRoutingRequestQueue;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import cotton.network.NetworkHandler;
import java.io.IOException;
import cotton.network.PathType;
import cotton.servicediscovery.CircuitBreakerPacket;
import cotton.servicediscovery.DiscoveryPacket;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import java.util.Set;
import java.util.ArrayList;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import cotton.systemsupport.UsageHistory;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Tony
 * @author Magnus
 */
/**
 * Manages the requestqueues.
 */
public class RequestQueueManager implements StatisticsProvider {

    private ConcurrentHashMap<String, RequestQueue> internalQueueMap;
    private ExecutorService threadPool;
    private InternalRoutingRequestQueue internalRouting;
    private int maxAmountOfQueues = 10;
    private ConcurrentHashMap<String,String> banList;
    
    public RequestQueueManager() {
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.banList = new ConcurrentHashMap<String,String>();

    }

    public RequestQueueManager(InternalRoutingRequestQueue internalRouting) {
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(100);//.newCachedThreadPool();
        this.internalRouting = internalRouting;
        this.banList = new ConcurrentHashMap<String,String>();
    }

    /**
     * A list of bannedServices that this node never can start a queue for.
     * It will not be possible to remove after added
     * @param bannedService a array of name of services can get a queue
     */
    public void addBanList(String[] bannedService) {
        for (int i = 0; i < bannedService.length; i++) {
            this.banList.put(bannedService[i], "b");
        }
    }
    /**
     * A array of names on different queues.
     *
     * @return activeQueues
     */
    public String[] getActiveQueues() {
        Set<String> keys = internalQueueMap.keySet();
        ArrayList<String> names = new ArrayList<>();
        names.addAll(keys);
        String[] nameList = names.toArray(new String[names.size()]);
        return nameList;
    }

    /**
     * initializes a specific queue for a specific service
     *
     * @param serviceName the name for a specific service.
     */
    public void startQueue(String serviceName) {
        if(this.banList.get(serviceName) != null)
            return;
        RequestQueue queuePool = new RequestQueue(serviceName, 100);
        internalQueueMap.putIfAbsent(serviceName, queuePool);
    }

    /**
     * Buffers data for processing
     *
     * @param packet A network packet containing the data to be processed
     * @param serviceName the name of the service that is supposed to process
     * this data.
     */
    public void queueService(NetworkPacket packet, String serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName);
        if (queue == null) {
            return;
        }
        queue.queueService(packet);
        if(queue.getThreadCount().get() < 5){
            threadPool.execute(queue);
        }
    }

    /**
     * Adds an available instance to the internal queue
     *
     * @param origin the instance that sent the message
     * @param serviceName the name of the service that is supposed to process
     * this data.
     */

    public void addAvailableInstance(Origin origin, String serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName);
        if (queue == null) {
            return;
        }
        //System.out.println("AvailableInstance: " + origin.getAddress().toString() + " :: " + serviceName);
        queue.addInstance(origin);
        if(queue.getThreadCount().get() < (queue.getMaxCapacity()/10)){
            threadPool.execute(queue);
        }   
    }

    public void stop() {
        threadPool.shutdown();
    }

    public StatisticsData[] getStatisticsForSubSystem(String serviceName) {
        ArrayList<StatisticsData> tdata = new ArrayList<>();
        for (Map.Entry<String, RequestQueue> rq : internalQueueMap.entrySet()) {
            tdata.add(rq.getValue().getStatistics());
        }
        return tdata.toArray(new StatisticsData[tdata.size()]);
    }

    public StatisticsData getStatistics(String[] serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName[0]);
        if (queue == null) {
            return new StatisticsData();
        }
        return queue.getStatistics();
    }
    
    @Override
    public StatisticsProvider getProvider() {
        return this;
    }

    @Override
    public StatType getStatType() {
        return StatType.REQUESTQUEUE;
    }
    public int getMaxCapacity(String serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName);
        return queue.getMaxCapacity();
    }

    public int getMaxAmountOfQueues() {
        return maxAmountOfQueues;
    }

    public void setMaxAmountOfQueues(int maxAmountOfQueues) {
        this.maxAmountOfQueues = maxAmountOfQueues;
    }

    public void setInternalRouting(InternalRoutingRequestQueue internalRouting){
        this.internalRouting = internalRouting;
    }
    
    private class RequestQueue implements Runnable {

        private ConcurrentLinkedQueue<NetworkPacket> processQueue;
        private ConcurrentLinkedQueue<Origin> processingNodes;
        private final String queueName;
        private int maxCapacity;
        private AtomicInteger threadCount = new AtomicInteger(0);
        private AtomicInteger inputCounter;
        private AtomicInteger outputCounter;
        private UsageHistory<TimeInterval> intervals;
        
        public RequestQueue(String queueName, int maxCapacity) {
            processQueue = new ConcurrentLinkedQueue<>();
            processingNodes = new ConcurrentLinkedQueue<>();
            this.queueName = queueName;
            this.maxCapacity = maxCapacity;
            this.intervals = new UsageHistory();
        }

        public StatisticsData getStatistics() {
            int[] data = {maxCapacity, processQueue.size(), processingNodes.size()};
            return new StatisticsData(StatType.REQUESTQUEUE, queueName, data);
        }

        public AtomicInteger getThreadCount() {
            return threadCount;
        }
        
        /**
         * buffers networkpackets to be processed
         *
         * @param packet a networkpacket to be processed
         */
        public void queueService(NetworkPacket packet) {
            if(maxCapacity < processQueue.size() && (processQueue.size()%100)==0){
                DiscoveryPacket discPacket = new DiscoveryPacket(DiscoveryPacketType.CIRCUITBREAKER);
                discPacket.setCircuitBreakerPacket(new CircuitBreakerPacket(queueName));
                internalRouting.notifyDiscovery(discPacket);
            }
            processQueue.add(packet);
        }

        /**
         * Adds an available instance to the internal queue
         *
         * @param origin the instance that sent the message
         */
        public void addInstance(Origin origin) {
            processingNodes.add(origin);
        }

        /**
         * polls data and sents it to available instances.
         *
         */
        public void run() {
            int thcount = this.threadCount.getAndIncrement();
            if( thcount > processingNodes.size() || thcount > processQueue.size()/2){
                this.threadCount.getAndDecrement();
                return;
            }
            
            Origin origin = null;
            while ((origin = processingNodes.poll()) != null) {
                NetworkPacket packet = processQueue.poll();
                if (packet == null) {
                    processingNodes.add(origin);
                    if(threadCount.get() > 1){
                        break;
                    }
                    continue;
                }
                packet.setPathType(PathType.SERVICE);
                try {
                    //networkHandler.send(packet,origin.getAddress());
                    internalRouting.sendWork(packet, origin.getAddress());
                    //System.out.println("Queue sent work to " + origin.getAddress().toString());
                } catch (IOException e) {
                    processQueue.add(packet);
                    // TODO: LOGGING
                }
            }
            this.threadCount.getAndDecrement();
        }

        public int getMaxCapacity() {
            return this.maxCapacity;
        }

    }
}
