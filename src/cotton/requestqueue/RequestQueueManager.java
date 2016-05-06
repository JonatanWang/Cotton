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

import cotton.network.NetworkPacket;
import cotton.network.Origin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import cotton.network.NetworkHandler;
import java.io.IOException;
import cotton.network.PathType;
import java.util.Set;
import java.util.ArrayList;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import java.util.Map;
import java.util.Set;
/**
 * @author Tony
 * @author Magnus
 */
/**
 * Manages the requestqueues.
 */
public class RequestQueueManager implements StatisticsProvider{

    private ConcurrentHashMap<String,RequestQueue> internalQueueMap;
    private ExecutorService threadPool;    
    private NetworkHandler networkHandler;

    public RequestQueueManager(){
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
    }

    public RequestQueueManager(NetworkHandler networkHandler){
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.networkHandler = networkHandler;
    }

    /**
     * Sets the netorkhandler so that the request queues can forward data for processing.
     * @param networkHandler sets the networkHandler 
     */
    public void setNetworkHandler(NetworkHandler networkHandler){
        this.networkHandler = networkHandler;
    }


    /**
     * A array of names on different queues.
     * @return activeQueues
     */
    public String[] getActiveQueues(){
        Set<String> keys = internalQueueMap.keySet();
        ArrayList<String> names = new ArrayList<>();
        names.addAll(keys);
        String[] nameList = names.toArray(new String[names.size()]);
        return nameList;
    }

    /**
     * initializes a specific queue for a specific service
     * @param serviceName the name for a specific service.
     */
    public void startQueue(String serviceName){
        RequestQueue queuePool = new RequestQueue(serviceName,100);
        internalQueueMap.putIfAbsent(serviceName,queuePool);
    }

    /**
     * Buffers data for processing
     * 
     * @param packet A network packet containing the data to be processed 
     * @param serviceName the name of the service that is supposed to process this data.
     */
    public void queueService(NetworkPacket packet,String serviceName){
        RequestQueue queue = internalQueueMap.get(serviceName);
        if(queue == null)
            return;
        queue.queueService(packet); 
        threadPool.execute(queue);
    }
    /**
     * Adds an available instance to the internal queue
     * 
     * @param origin the instance that sent the message 
     * @param serviceName the name of the service that is supposed to process this data.
     */

    public void addAvailableInstance(Origin origin,String serviceName){
        RequestQueue queue = internalQueueMap.get(serviceName);
        if(queue == null)
            return;
        //System.out.println("AvailableInstance: " + origin.getAddress().toString() + " :: " + serviceName);
        queue.addInstance(origin);
        threadPool.execute(queue);
    }

    public void stop(){
        threadPool.shutdown();
    }

    public StatisticsData[] getStatisticsForSubSystem(String serviceName){
        StatisticsData[] data= new StatisticsData[internalQueueMap.size()];
        int i =0;
        for(Map.Entry<String,RequestQueue> rq: internalQueueMap.entrySet()){
            data[i++] = rq.getValue().getStatistics();
        }
        return data;
    }

    public StatisticsData getStatistics(String[] serviceName){
        RequestQueue queue = internalQueueMap.get(serviceName[0]);
        if(queue == null)
            return new StatisticsData();
        return queue.getStatistics();
    }

    private class RequestQueue implements Runnable{
        private ConcurrentLinkedQueue<NetworkPacket> processQueue;
        private ConcurrentLinkedQueue<Origin> processingNodes;
        private final String queueName;
        private int maxCapacity;

        public RequestQueue(String queueName,int maxCapacity){
            processQueue = new ConcurrentLinkedQueue<>();
            processingNodes = new ConcurrentLinkedQueue<>();
            this.queueName = queueName;
            this.maxCapacity = maxCapacity;
        }

        public StatisticsData getStatistics(){
            int[] data = {maxCapacity,processQueue.size(),processingNodes.size()};
            return new StatisticsData(StatType.REQUESTQUEUE,queueName,data);
        }

        /**
         * buffers networkpackets to be processed
         *
         * @param packet a networkpacket to be processed
         */
        public void queueService(NetworkPacket packet){
            processQueue.add(packet);
        }
        /**
         * Adds an available instance to the internal queue
         * 
         * @param origin the instance that sent the message 
         */
        public void addInstance(Origin origin){
            processingNodes.add(origin);
        }

        /**
         * polls data and sents it to available instances. 
         *
         */
        public void run(){
            Origin origin = null;
            while((origin = processingNodes.poll()) != null){
                NetworkPacket packet = processQueue.poll();
                if(packet == null){
                    processingNodes.add(origin);
                    return;
                }
                packet.setPathType(PathType.SERVICE);
                try{
                    //networkHandler.send(packet,origin.getAddress());
                    networkHandler.sendOverActiveLink(packet, origin.getAddress());
                    //System.out.println("Queue sent work to " + origin.getAddress().toString());
                }catch(IOException e){
                    processQueue.add(packet);
                    // TODO: LOGGING
                }
            }
        }
    }
}