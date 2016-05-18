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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    public CloudStat() throws UnknownHostException {
        this.cloudLink = null;//new Cotton(false, null);
        //this.cloudLink.start();
        this.threadPool = Executors.newFixedThreadPool(3);

    }

    public void getStatData(String name, DataPusherGraph graph, DestinationMetaData destination,StatType type) {
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        this.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                Console console = cloudLink.getConsole();
                
                String[] queryRequest = new String[]{name, "isSampling"};
                Command getSample = new Command(type, name, queryRequest, 0, CommandType.USAGEHISTORY);
                //Command getSample = new Command(type, name, queryRequest, 0, CommandType.STATISTICS_FORSYSTEM);
                String startNumb = "0";
                String endNumb = "500";
                try {
                    StatisticsData[] sendQueryCommand = console.sendQueryCommand(getSample, dest);
                    if(sendQueryCommand != null && sendQueryCommand.length > 0){
                        Integer startPoint = sendQueryCommand[0].getNumberArray()[2];
                        startNumb = startPoint.toString();
                        Integer endPoint = startPoint + 500;
                        endNumb = endPoint.toString();
                    }
                } catch (IOException ex) {
                    Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                }
                Command command = new Command(type, null, new String[]{name, "setUsageRecordingInterval"}, 100, CommandType.USAGEHISTORY);
                //Command command = new Command(type, null, new String[]{name, "setUsageRecordingInterval"}, 100, CommandType.STATISTICS_FORSYSTEM);
                try {
                    console.sendCommand(command, dest);
                } catch (IOException ex) {
                    Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String[] cmdline = new String[]{name, "getUsageRecordingInterval", startNumb, endNumb};
                //Command query = new Command(type, null, cmdline, 200, CommandType.STATISTICS_FORSYSTEM);
                Command query = new Command(type, null, cmdline, 200, CommandType.USAGEHISTORY);
                query.setQuery(false);
                StatisticsData<TimeInterval>[] res = null;
                try {
                    res = console.sendQueryCommand(query, dest);
                } catch (IOException ex) {
                    Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(res == null) {
                    System.out.println("getStatData: fail, return null");
                    return;
                }
                if(res.length < 1){
                    System.out.println("getStatData: fail, return < 1");
                    return;
                }
                Command stopCmd = new Command(type, null, new String[]{name, "stopUsageRecording"}, 0, CommandType.USAGEHISTORY);
                //Command stopCmd = new Command(type, null, new String[]{name, "stopUsageRecording"}, 0, CommandType.STATISTICS_FORSYSTEM);
                
                try {
                    
                    console.sendCommand(stopCmd, dest);
                } catch (IOException ex) {
                    Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
                    return;
                }
                graph.pushData(name, res[0].getData());
                
            }
        });
    }

    public void setDelegate(LoggerController controller) {
        this.controller = controller;
    }

    public boolean resetCloudLink(String ipAddress, int port) {
        if (this.cloudLink != null) {
            this.cloudLink.shutdown();
        }
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress addr = new InetSocketAddress(ipAddress, port);
        gDns.setGlobalDiscoveryAddress(new SocketAddress[]{addr});
        try {
            this.cloudLink = new Cotton(false, gDns);
            this.cloudLink.start();
        } catch (UnknownHostException ex) {
            Logger.getLogger(CloudStat.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    public void shutDown() {
        this.threadPool.shutdown();
        if (this.cloudLink != null) {
            this.cloudLink.shutdown();
        }
    }

    public void updateGraph(String name, DataPusherGraph pusher) {

    }

    public ArrayList<StatisticsData<DestinationMetaData>> getNodes(StatType subSystem) {
        Console console = this.cloudLink.getConsole();
        ArrayList<StatisticsData<DestinationMetaData>> empty = new ArrayList();
        // get local discoverys connected dest
        StatisticsProvider d = console.getProvider(StatType.DISCOVERY);

        StatisticsData stat = d.getStatistics(new String[]{"discoveryNodes"});
        DestinationMetaData[] dests = (DestinationMetaData[]) stat.getData();
        if (dest == null||dests.length <= 0) {
            return empty;
        }
        // dirty hack
        System.out.println("Dests" + dests.toString());
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
