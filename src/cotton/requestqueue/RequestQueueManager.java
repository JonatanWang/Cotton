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

import cotton.configuration.QueueConfigurator;
import cotton.internalrouting.InternalRoutingRequestQueue;
import cotton.network.NetworkPacket;
import cotton.network.NetworkPacket.NetworkPacketBuilder;
import cotton.network.Origin;

import java.util.concurrent.*;
import java.io.IOException;
import cotton.network.PathType;
import cotton.servicediscovery.CircuitBreakerPacket;
import cotton.servicediscovery.DiscoveryPacket;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import cotton.servicediscovery.QueuePacket;
import cotton.systemsupport.ActivityLogger;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import java.util.Set;
import java.util.ArrayList;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import cotton.systemsupport.UsageHistory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.SocketAddress;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

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
    private AtomicInteger activeQueueCount = new AtomicInteger(0);
    private ConcurrentHashMap<String, String> banList;
    private SocketAddress localAddress = null;
    public RequestQueueManager() {
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();
        this.banList = new ConcurrentHashMap<String, String>();

    }

    public RequestQueueManager(QueueConfigurator config) {
        this.internalQueueMap = new ConcurrentHashMap<>();
        threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(100);//.newCachedThreadPool();
        this.banList = new ConcurrentHashMap<String, String>();
        this.maxAmountOfQueues = config.getQueueLimit();
        ArrayList<String> disabledServices = config.getDisabledServices();
        addBanList(disabledServices);
        System.out.println("Config :" + config.toString());
    }

    public RequestQueueManager(QueueConfigurator config, InternalRoutingRequestQueue internalRouting) {
        this.internalQueueMap = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(100);//.newCachedThreadPool();
        this.internalRouting = internalRouting;
        this.banList = new ConcurrentHashMap<String, String>();
        this.maxAmountOfQueues = config.getQueueLimit();
        ArrayList<String> disabledServices = config.getDisabledServices();
        addBanList(disabledServices);
        System.out.println("Config :" + config.toString());

    }

    public RequestQueueManager(InternalRoutingRequestQueue internalRouting) {
        this.internalQueueMap = new ConcurrentHashMap<>();
        this.threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(100);//.newCachedThreadPool();
        this.internalRouting = internalRouting;
        this.banList = new ConcurrentHashMap<String, String>();
    }

    /**
     * A list of bannedServices that this node never can start a queue for. It
     * will not be possible to remove after added
     *
     * @param bannedService a array of name of services can get a queue
     */
    public void addBanList(ArrayList<String> bannedService) {
        for (String banned : bannedService) {
            this.banList.put(banned, "b");
        }
    }

    /**
     * A list of bannedServices that this node never can start a queue for. It
     * will not be possible to remove after added
     *
     * @param bannedService a array of name of services can get a queue
     */
    public void addBanList(String[] bannedService) {
        for (int i = 0; i < bannedService.length; i++) {
            this.banList.put(bannedService[i], "b");
        }
    }

    public void setLocalAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
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
        if (this.banList.get(serviceName) != null || this.maxAmountOfQueues <= this.activeQueueCount.get()) {
            return;
        }
        RequestQueue queuePool = new RequestQueue(serviceName, 100);
        RequestQueue old = internalQueueMap.putIfAbsent(serviceName, queuePool);
        if(old != null) {
            if(old.getMaxCapacity() < 100){
                old.setMaxCapacity(100);
            }
            threadPool.execute(old);
        }else {
            threadPool.execute(queuePool);
        }
        this.activeQueueCount.incrementAndGet();
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
            // semd ot back again
            this.internalRouting.forwardResult(packet.getOrigin(), packet.getPath(), packet.getData());
            return;
        }
        queue.queueService(packet);
        if (queue.getThreadCount().get() < 5) {
            //        threadPool.execute(queue);
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
        //System.out.println("max capacity1: " + queue.getMaxCapacity());
        if (queue.getThreadCount().get() < (queue.getMaxCapacity())) {
            //  System.out.println("max capacity2: " + queue.getMaxCapacity());
            //     threadPool.execute(queue);
        }
    }

    public void stop() {
        for (Map.Entry<String, RequestQueue> entry : internalQueueMap.entrySet()) {
            entry.getValue().stopUsageRecording();
            entry.getValue().stop();
        }

        threadPool.shutdown();

    }
    /**
     * compares whether the socket addresses are equal.
     * @param other
     * @return
     */
    public StatisticsData[] getStatisticsForSubSystem(String serviceName) {
        ArrayList<StatisticsData> tdata = new ArrayList<>();
        for (Map.Entry<String, RequestQueue> rq : internalQueueMap.entrySet()) {
            tdata.add(rq.getValue().getStatistics(new String[]{serviceName, "queueData"}));
        }
        return tdata.toArray(new StatisticsData[tdata.size()]);
    }

    /**
     * returns statistics for a given queue.
     *
     * @param serviceName
     * @return
     */
    public StatisticsData getStatistics(String[] serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName[0]);
        if (queue == null) {
            return new StatisticsData();
        }
        return queue.getStatistics(serviceName);
    }
    /**
     * returns a statistics provider for the request queue.
     *
     * @return
     */
    @Override
    public StatisticsProvider getProvider() {
        return this;
    }
    /**
     * returns the subsystem that is statistics is requested for.
     *
     * @return
     */
    @Override
    public StatType getStatType() {
        return StatType.REQUESTQUEUE;
    }
    /**
     * return max capacity for a specific queue
     *
     * @param serviceName
     * @return
     */
    public int getMaxCapacity(String serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName);
        return queue.getMaxCapacity();
    }
    /**
     * return max capacity for a specific queue
     *
     * @param serviceName
     * @return
     */
    public int getMaxAmountOfQueues() {
        return maxAmountOfQueues;
    }
    /**
     * Sets the max amount of queues that the request queue manager can handle.
     *
     * @param maxAmountOfQueues
     */
    public void setMaxAmountOfQueues(int maxAmountOfQueues) {
        this.maxAmountOfQueues = maxAmountOfQueues;
    }
    /**
     * Sets an internal routing component so that the request queue can send information to be routed within the cloud.
     * @param internalRouting
     */
    public void setInternalRouting(InternalRoutingRequestQueue internalRouting) {
        this.internalRouting = internalRouting;
    }
    /** 
     * processes a command from the command and control unit.
     * @param command
     * @return 
     */
    public StatisticsData[] processCommand(Command command) {
        if(command.isQuery()) {
            return executeQuery(command);

        }else {
            executeCommand(command);
        }
        return null;
    }

    private StatisticsData[] executeQuery(Command command) {
        String[] tokens = command.getTokens();
        RequestQueue queue = null;
        switch (command.getCommandType()) {
            case STATISTICS_FORSUBSYSTEM:
                StatisticsData[] statisticsForSubSystem = this.getStatisticsForSubSystem(command.getName());
                break;
            case STATISTICS_FORSYSTEM:
                StatisticsData statistics = this.getStatistics(command.getTokens());
                break;
            case USAGEHISTORY:                
                if(tokens == null || tokens.length < 1) {
                    return null;
                }
                queue = internalQueueMap.get(tokens[0]);
                if (queue == null) {
                    return null;
                }
                StatisticsData ret = queue.getStatistics(command.getTokens());
                return new StatisticsData[]{ret};
            case CHANGE_ACTIVEAMOUNT:
                if(tokens == null || tokens.length < 1) {
                    return null;
                }
                int maxcap = 0;
                if(tokens[1].equals("getMaxQueueCount")){
                    maxcap = this.getMaxAmountOfQueues();
                    StatisticsData tmp = new StatisticsData(StatType.REQUESTQUEUE, tokens[0], new int[]{maxcap});
                    return new StatisticsData[]{tmp};
                }else if(tokens[1].equals("getQueueCount")) {
                    int qcount = this.activeQueueCount.get();
                    StatisticsData tmp = new StatisticsData(StatType.REQUESTQUEUE, tokens[0], new int[]{qcount});
                    return new StatisticsData[]{tmp};
                }
                queue = internalQueueMap.get(tokens[0]);
                if (queue == null) {
                    return null;
                }
                if (tokens[1].equals("getMaxCapacity")) {
                    maxcap = queue.getMaxCapacity();
                    StatisticsData tmp = new StatisticsData(StatType.REQUESTQUEUE, tokens[0], new int[]{maxcap});
                    return new StatisticsData[]{tmp};
                } 
                break;
            default:
                System.out.println("RequestQueueManager:executeQuery: commandType unknown: " + command.getCommandType().toString());
                break;
        }
        return new StatisticsData[0];
    }

    private void executeCommand(Command command) {
        CommandType commandType = command.getCommandType();
        if (commandType != CommandType.USAGEHISTORY && commandType != CommandType.CHANGE_ACTIVEAMOUNT) {
            return ;
        }
        String[] tokens = command.getTokens();
        if (tokens.length < 2) {
            return ;
        }
        String name = tokens[0];
        String task = tokens[1];
        int samplingRate = command.getAmount();
        if (task.equals("startQueue")) {
            this.startQueue(name);
            return;
        }
        RequestQueue queue = internalQueueMap.get(name);
        if (queue == null) {
            return ;
        }
        if (task.equals("setUsageRecordingInterval")) {
            queue.setUsageRecording(samplingRate);
        } else if (task.equals("setMaxCapacity")) {
            queue.setMaxCapacity(command.getAmount());
        } else if (task.equals("stopUsageRecording")) {
            queue.stopUsageRecording();
        } else if (task.equals("removeQueue")) {
            removeQueue(queue);
        } else {
            System.out.println("RequestQueueManager:executeCommand: task unknown: " + task);
        }
    }

    private void removeQueue(RequestQueue queue) {
        //steep 1, notify local discovery
        DiscoveryPacket discPacket = new DiscoveryPacket(DiscoveryPacketType.REQUESTQUEUE);
        QueuePacket queuePacket = new QueuePacket(this.localAddress,new String[]{queue.getName()});
        queuePacket.setShouldRemove(true);
        discPacket.setQueue(queuePacket);
        this.internalRouting.notifyDiscovery(discPacket);
        //step 2 , remove queue so all incoming packet gets redirected back to the cloud
        this.internalQueueMap.remove(queue.getName(), queue);
        // step 3 start empty all left over packets and pendings
        ArrayList<NetworkPacket> emptyAllPackets = queue.emptyAllPackets();
        ArrayList<Origin> emptyAllPendingRequests = queue.emptyAllPendingRequests();
        try {
            // step 4 send all left over packets to all the pending requests
            // also notify them that this is not in use any more
            byte[] data = serializeToBytes(discPacket);
            for (Origin pending : emptyAllPendingRequests) {
                NetworkPacket pkt = new NetworkPacketBuilder().setData(data).setPathType(PathType.DISCOVERY).setOrigin(new Origin()).build();
                internalRouting.sendWork(pkt, pending.getAddress());
                if (emptyAllPackets.isEmpty() == false) {
                    NetworkPacket item = emptyAllPackets.remove(0);
                    internalRouting.sendWork(item, pending.getAddress());
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(RequestQueueManager.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // step 5, all remaining packets arr sent back to the cloud
        for (NetworkPacket packet : emptyAllPackets) {
            this.internalRouting.forwardResult(packet.getOrigin(), packet.getPath(), packet.getData());
        }
        queue.stopUsageRecording();
        queue.stop();
    }
    
    private byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }
    
    private class RequestQueue implements Runnable {

        private BlockingQueue<NetworkPacket> processQueue;
        private BlockingQueue<Origin> processingNodes;
        private final String queueName;
        private int maxCapacity;
        private AtomicInteger threadCount = new AtomicInteger(0);
//        private AtomicInteger inputCounter;
//        private AtomicInteger outputCounter;
//        private UsageHistory usageHistory;
//        private Timer timer = new Timer();
//        private TimeSliceTask sliceTask = null;
        private ActivityLogger logger;
        private volatile boolean running = true;
        private int samplingRate = 200;

        public RequestQueue(String queueName, int maxCapacity) {
            this.processQueue = new LinkedBlockingQueue<>();
            this.processingNodes = new LinkedBlockingQueue<>();
            this.queueName = queueName;
            this.maxCapacity = maxCapacity;
            this.logger = new ActivityLogger(samplingRate);
//            this.usageHistory = new UsageHistory();
//            this.inputCounter = new AtomicInteger(0);
//            this.outputCounter = new AtomicInteger(0);
        }

        public String getName() {
            return this.queueName;
        }
        
        /**
         * getStatistics for this queue
         *
         * @return StatisticsData
         */
        public StatisticsData getStatistics(String[] statisticsInformation) {
            if (statisticsInformation.length < 2) {
                return new StatisticsData();
            }

            if (statisticsInformation[1].equals("queueData")) {
                int[] data = {maxCapacity, processQueue.size(), processingNodes.size()};
                return new StatisticsData(StatType.REQUESTQUEUE, queueName, data);
            } else if (statisticsInformation[1].equals("getUsageRecordingInterval")) {
                TimeInterval[] interval = null;
                if (statisticsInformation.length == 2) {
                    interval = this.logger.getUsageRecording(0, 0);
                } else if (statisticsInformation.length == 4) {
                    int first = Integer.parseInt(statisticsInformation[2]);
                    int last = Integer.parseInt(statisticsInformation[3]);
                    interval = this.logger.getUsageRecording(first, last);
                } else {
                    return new StatisticsData();
                }
                return new StatisticsData(StatType.REQUESTQUEUE, statisticsInformation[0], interval);
            } else if (statisticsInformation[1].equals("isSampling")) {
                if (hasRunningTimer()) {
                    return new StatisticsData(StatType.REQUESTQUEUE, statisticsInformation[0], new int[]{1, this.samplingRate, this.logger.getLastIndex()});
                }
                return new StatisticsData(StatType.REQUESTQUEUE, statisticsInformation[0], new int[]{0, this.samplingRate, this.logger.getLastIndex()});
            }
            return new StatisticsData();
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
            if (maxCapacity < processQueue.size() && (processQueue.size() % 100) == 0) {
                DiscoveryPacket discPacket = new DiscoveryPacket(DiscoveryPacketType.CIRCUITBREAKER);
                discPacket.setCircuitBreakerPacket(new CircuitBreakerPacket(queueName));
                internalRouting.notifyDiscovery(discPacket);
            }
            this.logger.recordInputEvent();
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
         * empty all pending requests
         * @return who was waiting on data
         */
        public ArrayList<Origin> emptyAllPendingRequests() {
            ArrayList<Origin> pendingRequests = new ArrayList<>();
            Origin origin = null;
            while((origin = processingNodes.poll()) != null) {
                pendingRequests.add(origin);
            }
            return pendingRequests;
        }
        
        /**
         * empty all queue packets
         * @return all data in the queue
         */
        public ArrayList<NetworkPacket> emptyAllPackets() {
            ArrayList<NetworkPacket> queuePackets = new ArrayList<>();
            NetworkPacket packet = null;
            while((packet = processQueue.poll()) != null) {
                queuePackets.add(packet);
            }
            return queuePackets;
        }
        
        /**
         * polls data and sents it to available instances.
         *
         */
        public void run() {
            int thcount = this.threadCount.getAndIncrement();
            if (thcount > processingNodes.size() || thcount > processQueue.size()) {
                this.threadCount.getAndDecrement();
                return;
            }

            Origin origin = null;
            do {
                try {
                    while ((origin = processingNodes.take()) != null) {
                        NetworkPacket packet = processQueue.take();
                        if (packet == null) {
                            processingNodes.add(origin);
                            if (threadCount.get() > 1) {
                                break;
                            }
                            continue;
                        }
                        packet.setPathType(PathType.SERVICE);
                        try {
                            //networkHandler.send(packet,origin.getAddress());

                            logger.recordOutputEvent();
                            internalRouting.sendWork(packet, origin.getAddress());
                            //System.out.println("Queue sent work to " + origin.getAddress().toString());
                        } catch (IOException e) {
                            processQueue.add(packet);
                            System.out.println("ERROR IN REQUEST QUEUE SEND WORK");
                            e.printStackTrace();
                            // TODO: LOGGING
                        }
                    }
                }catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (running);
            this.threadCount.getAndDecrement();
        }

        public int getMaxCapacity() {
            return this.maxCapacity;
        }
        
        public void setMaxCapacity(int newCap) {
            this.maxCapacity = newCap;
        }

        /**
         * Start the Usage History recording and sets the sampling rate
         *
         * @param samplingRate
         * @return
         */
        public boolean setUsageRecording(long samplingRate) {
            return this.logger.setUsageRecording(samplingRate);
        }

        /**
         * Stop the Usage History recording
         *
         * @return
         */
        public boolean stopUsageRecording() {
            return this.logger.stopUsageRecording();
        }

        public boolean hasRunningTimer() {
            return this.logger.hasRunningTimer();
        }

        public void stop() {
            running = false;
            logger.stop();
        }

//        private class TimeSliceTask extends TimerTask {
//
//            private long startTime;
//
//            public TimeSliceTask(long startTime) {
//                this.startTime = startTime;
//            }
//
//            @Override
//            public void run() {
//                long endTime = System.currentTimeMillis();
//                long deltaTime = endTime - startTime;
//                int in = inputCounter.get();
//                inputCounter.set(0);
//                int out = outputCounter.get();
//                outputCounter.set(0);
//                TimeInterval timeInterval = new TimeInterval(deltaTime);
//                timeInterval.setCurrentActiveCount(processQueue.size());
//                timeInterval.setInputCount(in);
//                timeInterval.setOutputCount(out);
//                usageHistory.add(timeInterval);
//                startTime = System.currentTimeMillis();
//            }
//        }

    }
}
