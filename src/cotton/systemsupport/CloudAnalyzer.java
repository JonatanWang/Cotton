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

    public void analyze() throws IOException {
        if (pool == null) {
            return;
        }
        DestinationMetaData[] destinations = pool.copyPoolData();
        ServiceRequest[] isSampling = new DefaultServiceRequest[destinations.length];
        String[] queryRequest = new String[]{name, "isSampling"};
        Command command = new Command(subSystemType, name, queryRequest, 0, CommandType.RECORD_USAGEHISTORY);
        command.setQuery(true);
        String[] samplingRequest = new String[]{name, "setUsageRecordingInterval"};
        
        Command newCommand = new Command(subSystemType,name,samplingRequest,100,CommandType.RECORD_USAGEHISTORY);
        byte[] data = serializeToBytes(command);
        byte[] tmp = serializeToBytes(newCommand);
        for (int i = 0; i < destinations.length; i++) {
            isSampling[i] = internalRouting.sendWithResponse(destinations[i], data, 80);
        }
        for (int i = 0; i < isSampling.length; i++) {
            byte[] response = isSampling[i].getData();
            if (response == null) {
                continue;
            }
            StatisticsData statisticsData = packetUnpack(response);
            if(statisticsData == null){
                continue;
            }
            int[] samples = statisticsData.getNumberArray();
            
            if(samples[0] == 1){
                internalRouting.sendToDestination(destinations[i], tmp);
            }
        }
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

}
