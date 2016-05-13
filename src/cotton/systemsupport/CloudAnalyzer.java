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

import cotton.internalRouting.DefaultServiceRequest;
import cotton.internalRouting.InternalRoutingServiceDiscovery;
import cotton.internalRouting.ServiceRequest;
import cotton.network.DestinationMetaData;
import cotton.servicediscovery.AddressPool;
import cotton.servicediscovery.GlobalServiceDiscovery;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Tony
 * @author Magnus
 */
public class CloudAnalyzer implements Serializable {

    private AddressPool pool;
    private String name;
    private StatType subSystemType;
    private InternalRoutingServiceDiscovery internalRouting;
    
    public CloudAnalyzer(AddressPool pool, String name, StatType subSystemType, InternalRoutingServiceDiscovery internalRouting) {
        this.pool = pool;
        this.name = name;
        this.subSystemType = subSystemType;
        this.internalRouting = internalRouting;
    }

    public void analyze(int samplingRate, int samplingPeriod) throws IOException {
        ArrayList<TimeInterval[]> statistics = gatherStatistics(samplingRate, samplingPeriod);
        Bucket[] buckets = calculateStatistics(statistics);
        double[] outInRatio = new double[buckets.length];
        double[] offeredTrafic = new double[buckets.length];
        double p0 = 0;
        for(int i =0; i<buckets.length; i++){
            outInRatio[i] = buckets[i].getOutputIntensities()/buckets[i].getInputIntensities();
            offeredTrafic[i] = buckets[i].getInputIntensities()/buckets[i].getOutputIntensities();
            
        }
        double totalTrafic = 0;
        double[] avgQueueSize = new double[statistics.size()];
        double min = 0;
        for(int i =0; i<outInRatio.length; i++){
            totalTrafic += outInRatio[i];
            if(min > outInRatio[i])
                min = outInRatio[i];
        }
        
        int butlers = pool.copyPoolData().length;
        for (int i = 0; i < buckets.length; i++) {
            if(butlers > 1.5*buckets[i].getQueueSize() && min > 0.30*offeredTrafic[i]){
                
            }
        }
        int j = 0;
        for(TimeInterval[] interval :statistics){
            for (int i = 0; i < interval.length; i++) {
                avgQueueSize[j] += interval[i].getCurrentQueueCount();
            }
            j++;
        }
        
        
        /*
        for (int i = 0; i < avgQueueSize.length; i++) {
           if(avgQueueSize[i] > totalTraffic){
               //SPAWN THREADS
           }else{
               //if(queuSize >= 0.7*totalTraffic)
                    //spawn queue
           } 
        }*/
        
        //TODO: finnish this method.
    }

    private Bucket[] calculateStatistics(ArrayList<TimeInterval[]> statisticsData) {
        Bucket[] buckets = new Bucket[statisticsData.size()];
        int i =0;
        for (TimeInterval[] timeIntervals : statisticsData) {
            int sampleLength = timeIntervals.length;
            double[] inputIntensity = new double[sampleLength];
            double[] outputIntensity = new double[sampleLength];
            long totalTime = 0;
            long inputCount = 0;
            long outputCount = 0;
            
            for (int j =0; j<sampleLength; j++) {
                inputIntensity[j] += timeIntervals[j].calculateInputIntensity();
                outputIntensity[j] += timeIntervals[j].calculateOutputIntensity();
                totalTime += timeIntervals[j].getDeltaTime();
                inputCount += timeIntervals[j].getInputCount();
                outputCount += timeIntervals[j].getOutputCount();
            }
            buckets[i] = new Bucket(inputIntensity[i]/sampleLength,outputIntensity[i]/sampleLength,
                    totalTime,inputCount,outputCount,timeIntervals[i].getCurrentQueueCount());
            i++;
            
        }
        return buckets;
    }

    private ArrayList<TimeInterval[]> gatherStatistics(int samplingRate, int samplingPeriod) throws IOException {
        if (pool == null) {
            return null;
        }
        DestinationMetaData[] destinations = pool.copyPoolData();
        ServiceRequest[] isSampling = new DefaultServiceRequest[destinations.length];
        String[] queryRequest = new String[]{name, "isSampling"};
        Command command = new Command(subSystemType, name, queryRequest, 0, CommandType.RECORD_USAGEHISTORY);
        command.setQuery(true);
        String[] samplingRequest = new String[]{name, "setUsageRecordingInterval"};

        Command newCommand = new Command(subSystemType, name, samplingRequest, samplingRate, CommandType.RECORD_USAGEHISTORY);

        byte[] data = serializeToBytes(command);
        byte[] startSampling = serializeToBytes(newCommand);
        int[] startIndex = new int[destinations.length];
        for (int i = 0; i < destinations.length; i++) {
            isSampling[i] = internalRouting.sendWithResponse(destinations[i], data, 80);
        }

        for (int i = 0; i < isSampling.length; i++) {
            byte[] response = isSampling[i].getData();
            if (response == null) {
                continue;
            }
            StatisticsData statisticsData = packetUnpack(response);
            if (statisticsData == null) {
                continue;
            }
            int[] samples = statisticsData.getNumberArray();

            startIndex[i] = samples[2];

            if (samples[0] == 1 || Math.abs(samples[1] - samplingRate) > 50) {
                internalRouting.sendToDestination(destinations[i], startSampling);
            }
        }

        for (int i = 0; i < 100; i++) {
            try {
                Thread.sleep(samplingPeriod);
            } catch (InterruptedException e) {

            }
        }

        ArrayList<TimeInterval[]> intervalCollection = new ArrayList<>();
        ServiceRequest[] timeIntervals = new ServiceRequest[destinations.length];
        for (int i = 0; i < destinations.length; i++) {
            String start = "" + startIndex[i];
            String end = "" + (startIndex[i] + (samplingPeriod / samplingRate));
            String[] collectRequests = new String[]{name, "getUsageRecordingInterval", start, end};

            Command collectSamples = new Command(subSystemType, name, collectRequests, samplingRate, CommandType.RECORD_USAGEHISTORY);
            byte[] serializedCommand = serializeToBytes(collectSamples);

            timeIntervals[i] = internalRouting.sendWithResponse(destinations[i], serializedCommand, 80);
        }

        for (int i = 0; i < timeIntervals.length; i++) {
            ServiceRequest timeInterval = timeIntervals[i];
            if (timeInterval == null) {
                continue;
            }
            byte[] tmp = timeInterval.getData();
            StatisticsData<TimeInterval> statistics = packetUnpack(tmp);
            intervalCollection.add(statistics.getData());
        }
        return intervalCollection;
    }

    private static byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    private static StatisticsData packetUnpack(byte[] data) {
        StatisticsData statistics = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            statistics = (StatisticsData) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GlobalServiceDiscovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        return statistics;
    }

    private class Bucket {

        private double inputIntensities;
        private double outputIntensities;
        private long inputAmount;
        private long outputAmount;
        private long totalTime;
        private long queueSize;
        public Bucket(double inputIntensities, double outputIntensities, long inputAmount, 
                long outputAmount, long totalTime,long queueSize) {
            this.inputIntensities = inputIntensities;
            this.outputIntensities = outputIntensities;
            this.inputAmount = inputAmount;
            this.outputAmount = outputAmount;
            this.totalTime = totalTime;
            this.queueSize = queueSize;
        }

        public long getQueueSize(){
            return this.queueSize;
        }
        
        public double getInputIntensities() {
            return inputIntensities;
        }

        public double getOutputIntensities() {
            return outputIntensities;
        }

        public long getInputAmount() {
            return inputAmount;
        }

        public long getOutputAmount() {
            return outputAmount;
        }

        public long getTotalTime() {
            return totalTime;
        }
    }
    
}
