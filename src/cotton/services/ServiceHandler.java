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
    private volatile boolean active = true;
    private ConcurrentHashMap<UUID, TimeSliceTask> timers;
    private ConcurrentHashMap<String, DataPath> bufferChannels = new ConcurrentHashMap<String, DataPath>();
    
    private Timer timeManger = new Timer();

    public ServiceHandler(ActiveServiceLookup serviceLookup, InternalRoutingServiceHandler internalRouting) {
        this.internalRouting = internalRouting;
        this.serviceLookup = serviceLookup;
        this.workBuffer = internalRouting.getServiceBuffer();
        this.threadPool = Executors.newCachedThreadPool();//.newFixedThreadPool(10);
        this.timers = new ConcurrentHashMap<UUID, TimeSliceTask>();
    }

    public void run() {
        while (active) {
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
                if(path != null) {
                    path.put(packet);
                }else {
                    path = new DataPath(nextServiceName);
                    DataPath oldPath = null;
                    oldPath = this.bufferChannels.putIfAbsent(nextServiceName, path);
                    if(oldPath != null) {
                        path = oldPath;
                    }
                    path.put(packet);
                    ServiceDispatcher th = new ServiceDispatcher(path);
                    threadPool.execute(th);
                }
                if(path.getListenerCount().get() <= service.getMaxCapacity()) {
                    ServiceDispatcher th = new ServiceDispatcher(path);
                    threadPool.execute(th);
                }
                
            }
        }
        threadPool.shutdownNow();
    }

    public void stop() {
        this.active = false;
        timers.clear();
        timeManger.cancel();
        timeManger.purge();
    }
    /**
     * Tells a specific service to change capacity. 
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
        for (int i = 0; i < diff; i++) {
            internalRouting.notifyRequestQueue(name);
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
        if (metaData == null) {
            return new StatisticsData();
        }

        if (cmdline[1].equals("serviceData")) {
            int[] data = {metaData.getMaxCapacity(), metaData.getCurrentThreadCount()};
            return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], data);
        } else if (cmdline[1].equals("getUsageRecordingInterval")) {
            TimeInterval[] interval = null;
            if (cmdline.length == 2) {
                interval = metaData.getUsageHistory().getUsageHistory();
            } else if (cmdline.length == 4) {
                int first = Integer.parseInt(cmdline[2]);
                int last = Integer.parseInt(cmdline[3]);
                ArrayList<TimeInterval> tmp = metaData.getUsageHistory().getInterval(first, last);
                interval = tmp.toArray(new TimeInterval[tmp.size()]);
            } else {
                return new StatisticsData();
            }
            return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], interval);
        } else if (cmdline[1].equals("isSampling")) {
            if (hasRunningTimer(metaData)) {
                return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], new int[]{1, (int) metaData.getSamplingRate(), metaData.getUsageHistory().getLastIndex()});
            }
            return new StatisticsData(StatType.SERVICEHANDLER, cmdline[0], new int[]{0, (int) metaData.getSamplingRate(), metaData.getUsageHistory().getLastIndex()});
        }

        return new StatisticsData();

    }
    /**
     * returns a statistics provider for the service handler.
     * return StatisticsProvider
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
            setUsageRecording(service, samplingRate);
        } else if (task.equals("stopUsageRecording")) {
            stopUsageRecording(service);
        } else {
            return false;
        }
        return true;
    }

    private StatisticsData[] parseQuery(Command command) {
        String[] tokens = command.getTokens();
        if (tokens != null && tokens.length > 1) {
            if(tokens[1].equals("getMaxCapacity")) {
                int maxcap = this.getServiceCapacity(tokens[0]);
                StatisticsData ret = new StatisticsData(StatType.SERVICEHANDLER,tokens[0],new int[]{maxcap});
                return new StatisticsData[]{ret};
            }
        }
        return new StatisticsData[0];
    }
    /**
     * processes a command that is received from the command and control unit.
     *
     */
    @Override
    public StatisticsData[] processCommand(Command command) {
        if (command.getCommandType() == CommandType.USAGEHISTORY) {
            if(command.isQuery()){
                StatisticsData res = getStatistics(command.getTokens());
                return new StatisticsData[]{res};
            
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
    public boolean setUsageRecording(ServiceMetaData service, long samplingRate) {
        TimeSliceTask timer = timers.get(service.getServiceId());
        service.setSamplingRate(samplingRate);
        if (timer != null) {
            timer.cancel();
        }
        service.setSampling(true);
        this.timeManger.scheduleAtFixedRate(new TimeSliceTask(service, System.currentTimeMillis()), 0, service.getSamplingRate());
        return true;
    }

    /**
     * Stop the Usage History recording
     *
     * @return
     */
    public boolean stopUsageRecording(ServiceMetaData service) {
        TimeSliceTask timer = timers.remove(service.getServiceId());
        service.setSampling(false);
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        return true;
    }
    /**
     * returns the type of subsystem that statistics is requested for
     *
     * @return
     */
    public boolean hasRunningTimer(ServiceMetaData service) {
        TimeSliceTask timer = timers.get(service.getServiceId());
        if (timer == null) {
            return false;
        }
        return true;

    }

    private class TimeSliceTask extends TimerTask {

        private long startTime;
        private ServiceMetaData service;

        public TimeSliceTask(ServiceMetaData service, long startTime) {
            this.startTime = startTime;
            this.service = service;
        }

        @Override
        public void run() {
            long endTime = System.currentTimeMillis();
            long deltaTime = endTime - startTime;
            int in = service.getInputCounter();
            service.setInputCounter(0);

            int out = service.getOutputCounter();
            service.setOutputCounter(0);
            TimeInterval timeInterval = new TimeInterval(deltaTime);
            timeInterval.setCurrentQueueCount(service.getCurrentThreadCount());
            timeInterval.setInputCount(in);
            timeInterval.setOutputCount(out);
            service.getUsageHistory().add(timeInterval);
            startTime = System.currentTimeMillis();
        }
    }

    private class DataPath {
        //private LinkedBlockingQueue<NetworkPacket> buffer;
        private ServiceBuffer buffer;
        private String name;
        private AtomicInteger listenerCount = new AtomicInteger(0);
        private AtomicBoolean stopOne = new AtomicBoolean(false);
        public DataPath(String name) {
            this.name = name;
            //this.buffer = new LinkedBlockingQueue<NetworkPacket>();
            this.buffer = new BridgeServiceBuffer();
        }

        public String getName() {
            return name;
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

        public ServiceDispatcher(DataPath dataPath) {
           this.dataPath = dataPath;
           this.serviceName = dataPath.getName();
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
                succesfulInit = false;
                return;
            }

            this.serviceFactory = serviceMetaData.getServiceFactory();

        }
        
        private void startService(NetworkPacket packet) {
            Service service = serviceFactory.newService();
            try {
                if (serviceMetaData.isSampling()) {
                    serviceMetaData.incrementInputCounter();
                }
                byte[] result = service.execute(null, packet.getOrigin(), packet.getData(), packet.getPath());

                internalRouting.forwardResult(packet.getOrigin(), packet.getPath(), result);
                serviceLookup.getService(serviceName).decrementThreadCount();
                internalRouting.notifyRequestQueue(serviceName);
                if (serviceMetaData.isSampling()) {
                    serviceMetaData.incrementOutputCounter();
                }
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
                for(int i = 0; i < 10; i++ ) {
                    try {
                         packet = this.dataPath.getPacket();
                         if(packet == null) {
                             break;
                         }
                         startService(packet);
                    } catch (InterruptedException ex) {}
                }
            }while(this.dataPath.getStopSignal() == false);
            serviceLookup.getService(serviceName).decrementThreadCount();
        }
    }
}
