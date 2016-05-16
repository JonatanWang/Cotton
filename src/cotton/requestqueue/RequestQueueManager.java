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
import cotton.network.Origin;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.io.IOException;
import cotton.network.PathType;
import cotton.servicediscovery.CircuitBreakerPacket;
import cotton.servicediscovery.DiscoveryPacket;
import cotton.servicediscovery.DiscoveryPacket.DiscoveryPacketType;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import java.util.Set;
import java.util.ArrayList;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import cotton.systemsupport.UsageHistory;
import org.apache.directory.shared.kerberos.codec.adKdcIssued.actions.StoreIRealm;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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
    private AtomicInteger activeQueueCount = new AtomicInteger(0);
    private ConcurrentHashMap<String, String> banList;

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
        internalQueueMap.putIfAbsent(serviceName, queuePool);
        threadPool.execute(queuePool);
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

    public StatisticsData[] getStatisticsForSubSystem(String serviceName) {
        ArrayList<StatisticsData> tdata = new ArrayList<>();
        for (Map.Entry<String, RequestQueue> rq : internalQueueMap.entrySet()) {
            tdata.add(rq.getValue().getStatistics(new String[]{serviceName, "queueData"}));
        }
        return tdata.toArray(new StatisticsData[tdata.size()]);
    }

    public StatisticsData getStatistics(String[] serviceName) {
        RequestQueue queue = internalQueueMap.get(serviceName[0]);
        if (queue == null) {
            return new StatisticsData();
        }
        return queue.getStatistics(serviceName);
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

    public void setInternalRouting(InternalRoutingRequestQueue internalRouting) {
        this.internalRouting = internalRouting;
    }

    public StatisticsData[] processCommand(Command command) {
        if(command.isQuery()) {

        }else {
            executeCommand(command);
            return null;
        }

        return null;
    }

    private StatisticsData[] executeQuery(Command command) {
        switch (command.getCommandType()) {
            case STATISTICS_FORSUBSYSTEM:
                StatisticsData[] statisticsForSubSystem = this.getStatisticsForSubSystem(command.getName());
                break;
            case STATISTICS_FORSYSTEM:
                StatisticsData statistics = this.getStatistics(command.getTokens());
                break;
            case USAGEHISTORY:
                String[] token = command.getTokens();
                if(token == null || token.length < 1) {
                    return null;
                }
                RequestQueue queue = internalQueueMap.get(token[0]);
                if (queue == null) {
                    return null;
                }
                StatisticsData ret = queue.getStatistics(command.getTokens());
                return new StatisticsData[]{ret};
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
        } else {
            System.out.println("RequestQueueManager:executeCommand: task unknown: " + task);
        }
    }

    private class RequestQueue implements Runnable {

        private ConcurrentLinkedQueue<NetworkPacket> processQueue;
        private ConcurrentLinkedQueue<Origin> processingNodes;
        private final String queueName;
        private int maxCapacity;
        private AtomicInteger threadCount = new AtomicInteger(0);
        private AtomicInteger inputCounter;
        private AtomicInteger outputCounter;
        private UsageHistory usageHistory;
        private Timer timer = new Timer();
        private TimeSliceTask sliceTask = null;
        private volatile boolean running = true;
        private int samplingRate = 0;

        public RequestQueue(String queueName, int maxCapacity) {
            this.processQueue = new ConcurrentLinkedQueue<>();
            this.processingNodes = new ConcurrentLinkedQueue<>();
            this.queueName = queueName;
            this.maxCapacity = maxCapacity;
            this.usageHistory = new UsageHistory();
            this.inputCounter = new AtomicInteger(0);
            this.outputCounter = new AtomicInteger(0);
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
                    interval = usageHistory.getUsageHistory();
                } else if (statisticsInformation.length == 4) {
                    int first = Integer.parseInt(statisticsInformation[2]);
                    int last = Integer.parseInt(statisticsInformation[3]);
                    ArrayList<TimeInterval> tmp = usageHistory.getInterval(first, last);
                    interval = tmp.toArray(new TimeInterval[tmp.size()]);
                } else {
                    return new StatisticsData();
                }
                return new StatisticsData(StatType.REQUESTQUEUE, statisticsInformation[0], interval);
            } else if (statisticsInformation[1].equals("isSampling")) {
                if (hasRunningTimer()) {
                    return new StatisticsData(StatType.REQUESTQUEUE, statisticsInformation[0], new int[]{1, this.samplingRate, usageHistory.getLastIndex()});
                }
                return new StatisticsData(StatType.REQUESTQUEUE, statisticsInformation[0], new int[]{0, this.samplingRate, usageHistory.getLastIndex()});
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
            inputCounter.incrementAndGet();
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
            if (thcount > processingNodes.size() || thcount > processQueue.size()) {
                this.threadCount.getAndDecrement();
                return;
            }

            Origin origin = null;
            do {
                while ((origin = processingNodes.poll()) != null) {
                    NetworkPacket packet = processQueue.poll();
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

                        outputCounter.incrementAndGet();
                        internalRouting.sendWork(packet, origin.getAddress());
                        //System.out.println("Queue sent work to " + origin.getAddress().toString());
                    } catch (IOException e) {
                        processQueue.add(packet);
                        System.out.println("ERROR IN REQUEST QUEUE SEND WORK");
                        // TODO: LOGGING
                    }
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
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
            this.samplingRate = (int) samplingRate;
            if (this.sliceTask != null) {
                sliceTask.cancel();

            }
            this.sliceTask = new TimeSliceTask(System.currentTimeMillis());
            timer.scheduleAtFixedRate(sliceTask, 0, this.samplingRate);
            return true;
        }

        /**
         * Stop the Usage History recording
         *
         * @return
         */
        public boolean stopUsageRecording() {
            if (sliceTask != null) {
                sliceTask.cancel();
            }
            return true;
        }

        public boolean hasRunningTimer() {
            if (timer == null) {
                return false;
            }
            return true;
        }

        public void stop() {
            running = false;
            if(this.sliceTask != null) {
                this.sliceTask.cancel();
            }
            timer.cancel();
            timer.purge();
            timer = null;
        }

        private class TimeSliceTask extends TimerTask {

            private long startTime;

            public TimeSliceTask(long startTime) {
                this.startTime = startTime;
            }

            @Override
            public void run() {
                long endTime = System.currentTimeMillis();
                long deltaTime = endTime - startTime;
                int in = inputCounter.get();
                inputCounter.set(0);
                int out = outputCounter.get();
                outputCounter.set(0);
                TimeInterval timeInterval = new TimeInterval(deltaTime);
                timeInterval.setCurrentQueueCount(processQueue.size());
                timeInterval.setInputCount(in);
                timeInterval.setOutputCount(out);
                usageHistory.add(timeInterval);
                startTime = System.currentTimeMillis();
            }
        }

    }
}
