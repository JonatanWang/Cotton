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
package cotton.systemsupport;

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
import cotton.test.services.GlobalDiscoveryAddress;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author magnus
 */
public class StatisticsRecorder {

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

    static void writeStatTest() throws UnknownHostException, FileNotFoundException, IOException {
        // GlobalDnsStub gDns = getDnsStub(null, 5888);
        GlobalDiscoveryAddress gDns = getDiscoveryAddress("127.0.0.1", 5888);
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
        SampleRange sample = isSampling(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE, null);
        if (!sample.isSampling()) {
            startRecording(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE, 100);
        }
        System.out.println("begining: " + sample.toString());
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(900);
            } catch (InterruptedException ex) {
            }
            System.out.println("begining: " + sample.toString());
            TimeInterval[] sampling = nextRecordingInterval(c, node.getData()[0], node.getName(), StatType.REQUESTQUEUE, sample);
            System.out.println("Sampling:" + node.getName() + " : " + StatType.REQUESTQUEUE.toString());
            System.out.println("Sample count: " + sampling.length);
            for (int j = 0; j < sampling.length; j++) {
                System.out.println("\t" + sampling[j].toString());
            }
        }

        c.shutdown();
    }

    public static class SampleRange {

        private int start;
        private int end;
        private boolean sampling;

        public SampleRange(int start, int end) {
            this.start = start;
            this.end = end;
            this.sampling = true;
        }

        public SampleRange(int start, int end, boolean sampling) {
            this.start = start;
            this.end = end;
            this.sampling = sampling;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public void setSampling(boolean sampling) {
            this.sampling = sampling;
        }

        public boolean isSampling() {
            return sampling;
        }

        void setStart(int start) {
            this.start = start;
        }

        void setEnd(int end) {
            this.end = end;
        }

        @Override
        public String toString() {
            return "Range{" + "start=" + start + ", end=" + end + '}';
        }

    }

    /**
     * this method can be called repeatedly , the range object is modified to reflect the new indexes
     * @param cloudLink the link to the cloud
     * @param destination node address
     * @param name name of the service
     * @param type what subsystem to get information from
     * @param range the last time sampling was called
     * @return array of discrete time interval of the usages for the subsystem  
     * @throws IOException 
     */
    public static TimeInterval[] nextRecordingInterval(Cotton cloudLink, DestinationMetaData destination, String name, StatType type, SampleRange range) throws IOException {
        SampleRange sr = isSampling(cloudLink, destination, name, type, range);
        if (sr == null) {
            return null;
        }
        if (!sr.isSampling()) {
            startRecording(cloudLink, destination, name, type, 100);
        }
        range.setStart(sr.getStart());
        range.setEnd(sr.getEnd());
        return getRecording(cloudLink, destination, name, type, range);
    }

    /**
     * 
     * @param cloudLink the link to the cloud
     * @param destination node address
     * @param name name of the service
     * @param type what subsystem to get information from
     * @param range the last time sampling was called, can be null
     * @return SampleRange , start and end index, adjusted to the range inputed, and if it is recording
     * @throws IOException 
     */
    public static SampleRange isSampling(Cotton cloudLink, DestinationMetaData destination, String name, StatType type, SampleRange range) throws IOException {
        if (range == null) {
            range = new SampleRange(0, 0);
        }
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        Console console = cloudLink.getConsole();

        String[] queryRequest = new String[]{name, "isSampling"};
        Command getSample = new Command(type, name, queryRequest, 0, CommandType.USAGEHISTORY);
        StatisticsData[] answer = console.sendQueryCommand(getSample, dest);
        int lastIdx = 0;
        boolean sampling = false;
        if (answer != null && answer.length > 0) {
            lastIdx = answer[0].getNumberArray()[2];
            sampling = (answer[0].getNumberArray()[0] == 1);
        } else {
            return null;
        }
        return new SampleRange(range.getEnd(), lastIdx, sampling);
    }

    /**
     * Start recording statistics with a given sample rate
     * @param cloudLink the link to the cloud
     * @param destination node address
     * @param name name of the service
     * @param type what subsystem to get information from
     * @param samplingRate what the length of every TimeInterval should be
     * @throws IOException 
     */
    public static void startRecording(Cotton cloudLink, DestinationMetaData destination, String name, StatType type, int samplingRate) throws IOException {
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        Console console = cloudLink.getConsole();

        String[] queryRequest = new String[]{name, "isSampling"};
        Command getSample = new Command(type, name, queryRequest, 0, CommandType.USAGEHISTORY);

        Command command = new Command(type, null, new String[]{name, "setUsageRecordingInterval"}, 100, CommandType.USAGEHISTORY);
        console.sendCommand(command, dest);
    }

    /**
     * 
     * @param cloudLink the link to the cloud
     * @param destination node address
     * @param name name of the service
     * @param type what subsystem to get information from
     * @param range the last time sampling was called, can be null
     * @return a array of TimeInterval for the given range
     * @throws IOException 
     */
    public static TimeInterval[] getRecording(Cotton cloudLink, DestinationMetaData destination, String name, StatType type, SampleRange range) throws IOException {
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

    /**
     * Gives back all the connected nodes to one of the many discovery nodes
     * @param cloudLink the cotton cloud link
     * @param subSystem what type of subsystem you want to get addresses for, ex:request queue, servicehandler
     * @return 
     */
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
            DestinationMetaData addr = new DestinationMetaData(dests[0].getSocketAddress(), PathType.COMMANDCONTROL);

            StatisticsData<DestinationMetaData>[] res = console.sendQueryCommand(command, addr);
            List<StatisticsData<DestinationMetaData>> val = Arrays.<StatisticsData<DestinationMetaData>>asList(res);
            result = new ArrayList<>(val);

        } catch (IOException ex) {

        }

        return result;
    }

    public static GlobalDiscoveryAddress getDiscoveryAddress(String dest, int port) throws UnknownHostException {
        GlobalDiscoveryAddress gDns = new GlobalDiscoveryAddress();
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
