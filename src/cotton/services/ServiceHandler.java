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

public class ServiceHandler implements Runnable, StatisticsProvider {

    private ActiveServiceLookup serviceLookup;
    private InternalRoutingServiceHandler internalRouting;
    private ServiceBuffer workBuffer;
    private ExecutorService threadPool;
    private volatile boolean active = true;
    private ConcurrentHashMap<UUID, TimeSliceTask> timers;
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
            ServicePacket packet = workBuffer.nextPacket();

            //TODO: if the threadcap met put packet back into buffer.
            if (packet == null) {
//                try{
//                    Thread.sleep(5); //change to exponential fallback strategy.
//                } catch (InterruptedException ex) {
//                }
            } else {
                ServiceDispatcher th = new ServiceDispatcher(packet);
                threadPool.execute(th);
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

    @Override
    public StatisticsProvider getProvider() {
        return this;
    }

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

    /**
     * ServiceDispatcher runs a service
     */
    private class ServiceDispatcher implements Runnable {

        private ServiceFactory serviceFactory;
        private boolean succesfulInit = true;
        private ServicePacket servicePacket;
        private String serviceName;
        private ServiceMetaData serviceMetaData;

        public ServiceDispatcher(ServicePacket servicePacket) {
            this.servicePacket = servicePacket;
            this.serviceName = this.servicePacket.getTo().getNextServiceName();

            if (this.serviceName == null) {
                succesfulInit = false;
                return;
            }

            this.serviceMetaData = serviceLookup.getService(serviceName);
            if (serviceMetaData == null) {
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

        @Override
        public void run() {
            if (succesfulInit == false) {
                return;
            }
            Service service = serviceFactory.newService();
            try {
                if (serviceMetaData.isSampling()) {
                    serviceMetaData.incrementInputCounter();
                }
                byte[] result = service.execute(null, servicePacket.getOrigin(), servicePacket.getData(), servicePacket.getTo());

                internalRouting.forwardResult(servicePacket.getOrigin(), servicePacket.getTo(), result);
                serviceLookup.getService(serviceName).decrementThreadCount();
                internalRouting.notifyRequestQueue(serviceName);
                if (serviceMetaData.isSampling()) {
                    serviceMetaData.incrementOutputCounter();
                }
            } catch (Exception e) {
                serviceLookup.getService(serviceName).decrementThreadCount();
                e.printStackTrace();
            }
        }
    }
}
