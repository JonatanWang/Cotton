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
package model;

import Controller.LoggerController;
import cotton.Cotton;
import cotton.internalrouting.InternalRoutingServiceDiscovery;
import cotton.internalrouting.ServiceRequest;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.TimeInterval;
import cotton.test.services.GlobalDnsStub;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import view.DataPusherGraph;

/**
 *
 * @author Magnus
 * @author Mats
 */
public class CloudStat {

    private Cotton cloudLink = null;
    private LoggerController controller;
    private ExecutorService threadPool;
    private ConcurrentHashMap<String, AtomicInteger> lastPosLookup = null;
    private AtomicBoolean running = new AtomicBoolean(false);
    public CloudStat() throws UnknownHostException {
        this.cloudLink = null;//new Cotton(false, null);
        //this.cloudLink.start();
        
        this.threadPool = Executors.newFixedThreadPool(3, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread th = new Thread(r);
                th.setDaemon(true);
                return th;
            }
        });//.newFixedThreadPool(3);
        
        this.running.set(true);
        this.lastPosLookup = new ConcurrentHashMap<String, AtomicInteger>();
    }

    public void getStatData(String name1, DataPusherGraph graph, DestinationMetaData destination, StatType type) {
        if(running.get() == false) {
            return;
        }
        final String name = name1;
        AtomicInteger tmp = this.lastPosLookup.get(name);
        if (tmp == null) {
            tmp = new AtomicInteger(0);
            AtomicInteger old = this.lastPosLookup.put(name, tmp);
            tmp = (old == null) ? tmp : old;
        }
        final AtomicInteger lastRead = tmp;
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        this.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                Console console = cloudLink.getConsole();

                String[] queryRequest = new String[]{name, "isSampling"};
                Command getSample = new Command(type, name, queryRequest, 0, CommandType.USAGEHISTORY);
                //Command getSample = new Command(type, name, queryRequest, 0, CommandType.STATISTICS_FORSYSTEM);

                String startNumb = Integer.toString(lastRead.get());

                String endNumb = null;
                boolean setUsage = true;
                int lastIdx = 0;
                try {
                    StatisticsData[] sendQueryCommand = console.sendQueryCommand(getSample, dest);
                    if (sendQueryCommand != null && sendQueryCommand.length > 0) {
                        lastIdx = sendQueryCommand[0].getNumberArray()[2];
                        //startNumb = startPoint.toString();
                        Integer sPoint =lastIdx;
                        startNumb = sPoint.toString();
                        Integer endPoint =lastIdx + 500;
                        endNumb = endPoint.toString();
                        setUsage = (sendQueryCommand[0].getNumberArray()[0] == 0);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                }
                Command command = new Command(type, null, new String[]{name, "setUsageRecordingInterval"}, 100, CommandType.USAGEHISTORY);
                //Command command = new Command(type, null, new String[]{name, "setUsageRecordingInterval"}, 100, CommandType.STATISTICS_FORSYSTEM);
                if (setUsage) {
                    try {
                        console.sendCommand(command, dest);
                    } catch (IOException ex) {
                        Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                        return;
                    }
                }
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String[] cmdline = null;
                if(endNumb == null || endNumb.equals("")) {
                    cmdline = new String[]{name, "getUsageRecordingInterval"};
                    
                }else {
                    cmdline = new String[]{name, "getUsageRecordingInterval", startNumb, endNumb};
                }
                System.out.println("Command: " + Arrays.toString(cmdline));
                //Command query = new Command(type, null, cmdline, 200, CommandType.STATISTICS_FORSYSTEM);
                Command query = new Command(type, null, cmdline, 200, CommandType.USAGEHISTORY);
                query.setQuery(false);
                StatisticsData<TimeInterval>[] res = null;
                try {
                    res = console.sendQueryCommand(query, dest);
                } catch (IOException ex) {
                    Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (res == null) {
                    System.out.println("getStatData: fail, return null");
                    return;
                }
                if (res.length < 1) {
                    System.out.println("getStatData: fail, return < 1");
                    return;
                }
                TimeInterval[] sampling = res[0].getData();
                System.out.println("Sampling:" + name + " : " + type.toString());

                System.out.println("Sample count: " + sampling.length);
                for (int i = 0; i < sampling.length; i++) {
                    System.out.println("\t" + sampling[i].toString());
                }

                lastRead.set(lastIdx + res[0].getData().length);
                graph.pushData(name, res[0].getData());

            }
        });
    }

    public void setDelegate(LoggerController controller) {
        this.controller = controller;
    }

    public boolean resetCloudLink(String ipAddress, int port) {
        if (this.cloudLink != null) {
            running.set(false);
            this.cloudLink.shutdown();
        }
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
        gDns.setGlobalDiscoveryAddress(new SocketAddress[]{addr});
        try {
            this.cloudLink = new Cotton(false, gDns);
            this.cloudLink.start();
            running.set(true);
        } catch (UnknownHostException ex) {
            Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return true;
    }

    public void shutDown() {
        this.running.set(false);
        if (this.cloudLink != null) {
            this.cloudLink.shutdown();
        }
        this.threadPool.shutdownNow();
    }

    public void updateGraph(String name, DataPusherGraph pusher) {

    }

    public ArrayList<StatisticsData<DestinationMetaData>> getNodes(StatType subSystem) {
        ArrayList<StatisticsData<DestinationMetaData>> empty = new ArrayList();
        if(running.get() == false) {
            return empty;
        }
        Console console = this.cloudLink.getConsole();
        // get local discoverys connected dest
        StatisticsProvider d = console.getProvider(StatType.DISCOVERY);

        StatisticsData stat = d.getStatistics(new String[]{"discoveryNodes"});
        DestinationMetaData[] dests = (DestinationMetaData[]) stat.getData();
        if (dests == null || dests.length <= 0) {
            return empty;
        }
        // dirty hack
        System.out.println("Dests: " + Arrays.toString(dests));
        String cmdString = null;
        switch (subSystem) {
            case DISCOVERY:
                cmdString = "discoveryNodes";
                break;
            case REQUESTQUEUE:
                cmdString = "requestQueueNodes";
                break;
            case SERVICEHANDLER:
                cmdString = "serviceNodes";
                break;
            default:
                return empty;
        }

        Command command = new Command(StatType.DISCOVERY, cmdString, null, 0, CommandType.STATISTICS_FORSUBSYSTEM);
        command.setQuery(true);
        StatisticsData[] query = null;
        ArrayList<StatisticsData<DestinationMetaData>> result = null;
        try {
            //byte[] data = serializeToBytes(command);
            //dests[0].setPathType(PathType.COMMANDCONTROL);
            DestinationMetaData addr = new DestinationMetaData(dests[0].getSocketAddress(), PathType.COMMANDCONTROL);

            StatisticsData<DestinationMetaData>[] res = console.sendQueryCommand(command, addr);
            List<StatisticsData<DestinationMetaData>> val = Arrays.<StatisticsData<DestinationMetaData>>asList(res);
            result = new ArrayList<>(val);

        } catch (IOException ex) {
            Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }

    private static byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    private StatisticsData[] packetUnpack(byte[] data) {
        StatisticsData[] statistics = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            statistics = (StatisticsData[]) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return statistics;
    }
}
