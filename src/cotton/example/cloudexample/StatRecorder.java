/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.example.cloudexample;

import cotton.Cotton;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.systemsupport.Command;
import cotton.systemsupport.CommandType;
import cotton.systemsupport.Console;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsProvider;
import cotton.systemsupport.TimeInterval;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author o_0
 */
public class StatRecorder {

    public static void writeAddressToFile(FileOutputStream f, ArrayList<StatisticsData<DestinationMetaData>> nodes) throws IOException {
        for (StatisticsData<DestinationMetaData> node : nodes) {
            StringBuilder sb = new StringBuilder();
            sb.append("Name: " + node.getName() + "\n");
            DestinationMetaData[] dest = node.getData();
            sb.append("\tTotal Address:" + dest.length + "\n");
            int count = 0;
            for (DestinationMetaData d : dest) {
                count++;
                sb.append("\t" + count + " : " + d.toString() + "\n");
            }
            byte[] data = sb.toString().getBytes();
            f.write(data);
        }
    }

    public static void main(String[] args) throws UnknownHostException, FileNotFoundException, IOException {
        GlobalDnsStub gDns = getDnsStub(null, 5888);
        Cotton c = new Cotton(false, gDns);
        c.start();
        FileOutputStream f = new FileOutputStream("StatData_test1.txt");
        ArrayList<StatisticsData<DestinationMetaData>> nodes = null;
        nodes = getNodes(c, StatType.SERVICEHANDLER);
        writeAddressToFile(f, nodes);
        nodes = getNodes(c, StatType.REQUESTQUEUE);
        StatisticsData<DestinationMetaData> node = nodes.get(0);
        writeAddressToFile(f, nodes);
        nodes = getNodes(c, StatType.DISCOVERY);
        writeAddressToFile(f, nodes);
        f.close();
        System.out.println("Type; " + node.getData()[0].getPathType().toString());//.getType().toString());
        startSampling(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE);
        Range sample = isSampling(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE,null);
        System.out.println("begining: " + sample.toString());    
        try {
            Thread.sleep(900);
        } catch (InterruptedException ex) {}
        sample = isSampling(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE,sample);
        System.out.println("begining: " + sample.toString());
        TimeInterval[] sampling = getSampling(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE, sample);
        System.out.println("Sampling:" + node.getName() + " : " + StatType.REQUESTQUEUE.toString());    
        
        System.out.println("Sample count: " + sampling.length);
        for (int i = 0; i < sampling.length; i++) {
            System.out.println("\t" + sampling[i].toString());
        }
        c.shutdown();
    }

    private static class Range {

        private int start;
        private int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "Range{" + "start=" + start + ", end=" + end + '}';
        }

    }

    public static Range isSampling(Cotton cloudLink, DestinationMetaData destination, String name, StatType type, Range range) throws IOException {
        if(range == null) {
            range = new Range(0,0);
        }
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        Console console = cloudLink.getConsole();

        String[] queryRequest = new String[]{name, "isSampling"};
        Command getSample = new Command(type, name, queryRequest, 0, CommandType.USAGEHISTORY);
        StatisticsData[] sendQueryCommand = console.sendQueryCommand(getSample, dest);
        int lastIdx = 0;
        if (sendQueryCommand != null && sendQueryCommand.length > 0) {
            lastIdx = sendQueryCommand[0].getNumberArray()[2];
            
        }
        return new Range(range.getEnd(),lastIdx);
    }

    public static void startSampling(Cotton cloudLink, DestinationMetaData destination, String name, StatType type) throws IOException {
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        Console console = cloudLink.getConsole();

        String[] queryRequest = new String[]{name, "isSampling"};
        Command getSample = new Command(type, name, queryRequest, 0, CommandType.USAGEHISTORY);

        Command command = new Command(type, null, new String[]{name, "setUsageRecordingInterval"}, 100, CommandType.USAGEHISTORY);
        console.sendCommand(command, dest);
    }

    public static TimeInterval[] getSampling(Cotton cloudLink, DestinationMetaData destination, String name, StatType type, Range range) throws IOException {
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        Console console = cloudLink.getConsole();
        String[] cmdline = null;
        if (range == null) {
            cmdline = new String[]{name, "getUsageRecordingInterval"};
        } else {
            int start = range.getStart();
            int end = range.getEnd();
            end = (end <= 0) ? start + 500 : end;
            cmdline = new String[]{name, "getUsageRecordingInterval", "" + start, "" + end};
        }

        System.out.println("Command: " + Arrays.toString(cmdline));
        //Command query = new Command(type, null, cmdline, 200, CommandType.STATISTICS_FORSYSTEM);
        Command query = new Command(type, null, cmdline, 200, CommandType.USAGEHISTORY);
        query.setQuery(false);
        StatisticsData<TimeInterval>[] res = null;
        res = console.sendQueryCommand(query, dest);

        if (res == null) {
            System.out.println("getStatData: fail, return null");
            return new TimeInterval[0];
        }
        if (res.length < 1) {
            System.out.println("getStatData: fail, return < 1");
            return new TimeInterval[0];
        }
        return res[0].getData();
    }

    public static ArrayList<StatisticsData<DestinationMetaData>> getNodes(Cotton cloudLink, StatType subSystem) {
        ArrayList<StatisticsData<DestinationMetaData>> empty = new ArrayList();

        Console console = cloudLink.getConsole();
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

        }

        return result;
    }

    private static GlobalDnsStub getDnsStub(String dest, int port) throws UnknownHostException {
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress gdAddr = null;
        if (dest == null) {
            gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
        } else {
            gdAddr = new InetSocketAddress(dest, port);
        }
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
