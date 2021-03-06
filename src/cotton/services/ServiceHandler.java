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

import cotton.internalrouting.InternalRoutingServiceHandler;
import cotton.network.NetworkPacket;
import cotton.systemsupport.ActivityLogger;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatType;
import cotton.systemsupport.TimeInterval;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceHandler implements Runnable, StatisticsProvider {

    private ActiveServiceLookup serviceLookup;
    private InternalRoutingServiceHandler internalRouting;
    private ServiceBuffer workBuffer;
    private ExecutorService threadPool;
    private AtomicBoolean active = new AtomicBoolean(true);
//    private ConcurrentHashMap<UUID, ActivityLogger> activityLogger;
    private ConcurrentHashMap<String, DataPath> bufferChannels = null;

    public ServiceHandler(ActiveServiceLookup serviceLookup, InternalRoutingServiceHandler internalRouting) {
        this.internalRouting = internalRouting;
        this.serviceLookup = serviceLookup;
        this.workBuffer = internalRouting.getServiceBuffer();
        this.threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(10);
        this.bufferChannels = new ConcurrentHashMap<String, DataPath>();
        //fillBufferChannels();
//        this.activityLogger = new ConcurrentHashMap<UUID, ActivityLogger>();
    }

    public void fillBufferChannels() {

        for (Map.Entry<String, ServiceMetaData> entry : this.serviceLookup.getEntrySet()) {
            String key = entry.getKey();
            DataPath p = new DataPath(key);
            this.bufferChannels.putIfAbsent(key, p);
        }
    }

    public void run() {
        fillBufferChannels();
        while (active.get()) {
            NetworkPacket packet = workBuffer.nextPacket();
            String nextServiceName = packet.getPath().getNextServiceName();
            ServiceMetaData service = this.serviceLookup.getService(nextServiceName);
            //TODO: if the threadcap met put packet back into buffer.
            if (service == null) {
//                try{
//                    Thread.sleep(5); //change to exponential fallback strategy.
//                } catch (InterruptedException ex) {
//                }
            } else {
                DataPath path = bufferChannels.get(nextServiceName);
                if (path != null) {
                    path.put(packet);
                } else {
                    path = new DataPath(nextServiceName);
                    DataPath oldPath = null;
                    oldPath = this.bufferChannels.putIfAbsent(nextServiceName, path);
                    if (oldPath != null) {
                        path = oldPath;
                    }
                    path.put(packet);
                    ServiceDispatcher th = new ServiceDispatcher(path);
                    threadPool.execute(th);
                }
                if (path.getListenerCount().get() <= service.getMaxCapacity()) {
                    ServiceDispatcher th = new ServiceDispatcher(path);
                    threadPool.execute(th);
                }

            }
        }
        threadPool.shutdownNow();
    }

    public void stop() {
        this.active.set(false);
        for (Map.Entry<String, DataPath> entry : this.bufferChannels.entrySet()) {
            DataPath value = entry.getValue();
            value.getActivityLogger().stop();
        }
        threadPool.shutdownNow();
        this.bufferChannels.clear();
    }

    /**
     * Tells a specific service to change capacity.
     *
     * @param name
     * @param amount
     */
    public void setServiceConfig(String name, int amount) {
        ServiceMetaData service = serviceLookup.getService(name);
        if (service == null) {
            return;
        }
        int diff = amount - service.getMaxCapacity();
        service.setMaxCapacity(amount);
        DataPath path = this.bufferChannels.get(name);
        for (int i = 0; i < diff; i++) {
            internalRouting.notifyRequestQueue(name);
            if(path != null) {
                ServiceDispatcher th = new ServiceDispatcher(path);
                threadPool.execute(th);
            }
        }
    }

    public int getServiceCapacity(String name) {
        ServiceMetaData service = serviceLookup.getService(name);
        if (service == null) {
            return 0;
        }
        return service.getMaxCapacity();
    }

    /*
     *   Statistics 
     */
    public StatisticsData[] getStatisticsForSubSystem(String name) {
        ArrayList<StatisticsData> result = new ArrayList<>();
        for (Map.Entry<String, ServiceMetaData> entry : serviceLookup.getEntrySet()) {
            ServiceMetaData metaData = entry.getValue();
            int[] data = {metaData.getMaxCapacity(), metaData.getCurrentThreadCount()};
            result.add(new StatisticsData(StatType.SERVICEHANDLER, entry.getKey(), data));
        }
        StatisticsData[] ret = result.toArray(new StatisticsData[result.size()]);
        return ret;
    }

    /**
     * returns statistics for a specific service handler.
     *
     * @param cmdline
     * @return
     */
    @Override
    public StatisticsData getStatistics(String[] cmdline) {
        ServiceMetaData metaData = serviceLookup.getService(cmdline[0]);
        UUID serviceId = metaData.getServiceId();
        if (metaData == null) {
            return new StatisticsData();
        }
        DataPath p = this.bufferChannels.get(cmdline[0]);
        ActivityLogger logger = null;
        if (p != null) {
            logger = p.getActivityLogger();
        }
        if (cmdline[1].equals("serviceData")) {
            int[] data = {metaData.getMaxCapacity(), metaData.getCurrentThreadCount()};
            return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], data);
        } else if (logger == null) {
            return new StatisticsData();
        } else if (cmdline[1].equals("getUsageRecordingInterval")) {
            TimeInterval[] interval = null;
            if (cmdline.length == 2) {
                interval = logger.getUsageRecording(0, 0);
            } else if (cmdline.length == 4) {
                int first = Integer.parseInt(cmdline[2]);
                int last = Integer.parseInt(cmdline[3]);
                interval = logger.getUsageRecording(first, last);
            } else {
                return new StatisticsData();
            }
            return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], interval);
        } else if (cmdline[1].equals("isSampling")) {
            if (logger == null) {
                return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], new int[]{0, 0, 0});
            }
            int on = (logger.hasRunningTimer()) ? 1 : 0;
            return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], new int[]{on, (int) logger.getSamplingRate(), logger.getLastIndex()});
        }

        return new StatisticsData();

    }

    /**
     * returns a statistics provider for the service handler. return
     * StatisticsProvider
     */
    @Override
    public StatisticsProvider getProvider() {
        return this;
    }

    /**
     * returns the type of subsystem that statistics is requested for
     *
     * @return
     */
    @Override
    public StatType getStatType() {
        return StatType.SERVICEHANDLER;
    }

    private boolean usageRecording(Command command) {
        CommandType commandType = command.getCommandType();
        if (commandType != CommandType.USAGEHISTORY) {
            return false;
        }
        String[] tokens = command.getTokens();
        if (tokens.length < 2) {
            return false;
        }
        String name = tokens[0];
        String task = tokens[1];
        int samplingRate = command.getAmount();
        //RequestQueue queue = internalQueueMap.get(name);
        ServiceMetaData service = this.serviceLookup.getService(name);
        if (service == null) {
            return false;
        }
        if (task.equals("setUsageRecordingInterval")) {
            setUsageRecording(name, samplingRate);
        } else if (task.equals("stopUsageRecording")) {
            stopUsageRecording(name);
        } else {
            return false;
        }
        return true;
    }

    private StatisticsData[] parseQuery(Command command) {
        String[] tokens = command.getTokens();
        if (tokens != null && tokens.length > 1) {
            if (tokens[1].equals("getMaxCapacity")) {
                int maxcap = this.getServiceCapacity(tokens[0]);
                StatisticsData ret = new StatisticsData(StatType.SERVICEHANDLER, tokens[0], new int[]{maxcap});
                return new StatisticsData[]{ret};
            }
        }
        return new StatisticsData[0];
    }

    private StatisticsData[] usageQuery(Command command) {
        String[] tokens = command.getTokens();
        if (tokens == null || tokens.length < 1) {
            return new StatisticsData[0];
        }
        if (tokens[1].equals("getMaxCapacity")) {
            int maxcap = this.getServiceCapacity(tokens[0]);
            StatisticsData ret = new StatisticsData(StatType.SERVICEHANDLER, tokens[0], new int[]{maxcap});
            return new StatisticsData[]{ret};
        }
        DataPath p = this.bufferChannels.get(tokens[0]);
        if (p == null) {
            return new StatisticsData[0];
        }
        TimeInterval[] interval = null;
        ActivityLogger logger = p.getActivityLogger();
        if (tokens[1].equals("getUsageRecordingInterval") && tokens.length == 2) {
            interval = logger.getUsageRecording(0, 0);
        } else if (tokens.length == 4) {
            int first = Integer.parseInt(tokens[2]);
            int last = Integer.parseInt(tokens[3]);
            interval = logger.getUsageRecording(first, last);
        }else if (tokens[1].equals("isSampling")) {
            int on = (logger.hasRunningTimer()) ? 1 : 0;
            StatisticsData ret = new StatisticsData(StatType.SERVICEHANDLER, tokens[0], new int[]{on, (int) logger.getSamplingRate(), logger.getLastIndex()});
            return new StatisticsData[]{ret};
        } else {
            return new StatisticsData[0];
        }
        return new StatisticsData[]{new StatisticsData(StatType.SERVICEHANDLER, tokens[0], interval)};
    }

    /**
     * processes a command that is received from the command and control unit.
     *
     */
    @Override
    public StatisticsData[] processCommand(Command command) {
        if (command.getCommandType() == CommandType.USAGEHISTORY) {
            if (command.isQuery()) {
                return usageQuery(command);
            }
            usageRecording(command);
            return new StatisticsData[0];
        } else if (command.getCommandType() == CommandType.CHANGE_ACTIVEAMOUNT) {
            if (command.isQuery()) {
                return parseQuery(command);
            }

            setServiceConfig(command.getName(), command.getAmount());
            return new StatisticsData[0];
        }
        return null;
    }

    /**
     * Start the Usage History recording and sets the sampling rate
     *
     * @param samplingRate
     * @return
     */
    public boolean setUsageRecording(String serviceName, long samplingRate) {
        DataPath p = this.bufferChannels.get(serviceName);
        if (p == null) {
            return false;
        }
        return p.getActivityLogger().setUsageRecording(samplingRate);
    }

    /**
     * Stop the Usage History recording
     *
     * @return
     */
    public boolean stopUsageRecording(String serviceName) {
        DataPath p = this.bufferChannels.get(serviceName);
        if (p == null) {
            return false;
        }
        return p.getActivityLogger().stopUsageRecording();
    }

    /**
     * returns the type of subsystem that statistics is requested for
     *
     * @return
     */
    public boolean hasRunningTimer(String serviceName) {
        DataPath p = this.bufferChannels.get(serviceName);
        if (p == null) {
            return false;
        }
        return p.getActivityLogger().hasRunningTimer();
    }

    private class DataPath {

        //private LinkedBlockingQueue<NetworkPacket> buffer;
        private ServiceBuffer buffer;
        private String name;
        private AtomicInteger listenerCount = new AtomicInteger(0);
        private AtomicBoolean stopOne = new AtomicBoolean(false);
        private ActivityLogger logger = new ActivityLogger(200);

        public DataPath(String name) {
            this.name = name;
            //this.buffer = new LinkedBlockingQueue<NetworkPacket>();
            this.buffer = new BridgeServiceBuffer();
        }

        public String getName() {
            return name;
        }

        public ActivityLogger getActivityLogger() {
            return this.logger;
        }

        public AtomicInteger getListenerCount() {
            return listenerCount;
        }

        public boolean put(NetworkPacket pkt) {
            //return this.buffer.offer(pkt);
            return this.buffer.add(pkt);
        }

        public NetworkPacket getPacket() throws InterruptedException {
            //return this.buffer.take();
            return this.buffer.nextPacket();
        }

        public void stopOneThread() {
            this.stopOne.set(true);
        }

        public boolean getStopSignal() {
            return this.stopOne.getAndSet(false);
        }

    }

    /**
     * ServiceDispatcher runs a service
     */
    private class ServiceDispatcher implements Runnable {

        private ServiceFactory serviceFactory;
        private boolean succesfulInit = true;
        private String serviceName;
        private ServiceMetaData serviceMetaData;
        private DataPath dataPath;
        private ActivityLogger logger;

        public ServiceDispatcher(DataPath dataPath) {
            this.dataPath = dataPath;
            this.serviceName = dataPath.getName();
            logger = this.dataPath.getActivityLogger();
            dataPath.getListenerCount().incrementAndGet();
            if (this.serviceName == null) {
                dataPath.getListenerCount().decrementAndGet();
                succesfulInit = false;
                return;
            }

            this.serviceMetaData = serviceLookup.getService(serviceName);
            if (serviceMetaData == null) {
                dataPath.getListenerCount().decrementAndGet();
                succesfulInit = false;
                return;
            }

            int currentServiceCount = serviceMetaData.incrementThreadCount();
            if (currentServiceCount > serviceMetaData.getMaxCapacity()) {
                serviceMetaData.decrementThreadCount();
                dataPath.getListenerCount().decrementAndGet();
                succesfulInit = false;
                return;
            }

            this.serviceFactory = serviceMetaData.getServiceFactory();

        }

        private void startService(NetworkPacket packet) {
            Service service = serviceFactory.newService();
            try {
                this.logger.recordInputEvent();
                byte[] result = service.execute(null, packet.getOrigin(), packet.getData(), packet.getPath());

                internalRouting.forwardResult(packet.getOrigin(), packet.getPath(), result);
                serviceLookup.getService(serviceName).decrementThreadCount();
                internalRouting.notifyRequestQueue(serviceName);
                this.logger.recordOutputEvent();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            if (succesfulInit == false) {
                return;
            }
            NetworkPacket packet = null;
            do {
                for (int i = 0; i < 10; i++) {
                    try {
                        packet = this.dataPath.getPacket();
                        if (packet == null) {
                            break;
                        }
                        startService(packet);
                    } catch (InterruptedException ex) {
                    }
                }
            } while (this.dataPath.getStopSignal() == false);
            serviceLookup.getService(serviceName).decrementThreadCount();
        }
    }
}
