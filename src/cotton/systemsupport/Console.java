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


import com.sun.org.apache.bcel.internal.generic.BREAKPOINT;
import cotton.internalrouting.InternalRoutingServiceDiscovery;
import cotton.internalrouting.ServiceRequest;
import cotton.network.DestinationMetaData;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.requestqueue.RequestQueueManager;
import cotton.servicediscovery.DiscoveryPacket;
import cotton.servicediscovery.GlobalServiceDiscovery;
import cotton.servicediscovery.ServiceDiscovery;
import cotton.services.ServiceHandler;
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
 * @author Magnus ,Tony
 * @author Mats
 */
public class Console {

    private ArrayList<StatisticsProvider> subSystems;

    public Console(StatisticsProvider[] subSystems) {
        this.subSystems = new ArrayList<>();
        this.subSystems.addAll(Arrays.asList(subSystems));
    }

    public Console(ArrayList<StatisticsProvider> subSystems) {
        this.subSystems = subSystems;
    }

    public Console() {
        this.subSystems = new ArrayList<>();
    }

    public void addSubSystem(StatisticsProvider system) {
        this.subSystems.add(system);
    }

    public StatisticsProvider getProvider(StatType type) {
        for (StatisticsProvider p : subSystems) {
            if (p.getStatType() == type) {
                return p;
            }
        }
        return new EmptyProvider("Unknown provider: " + type.toString());
    }

    /**
     * Process a command locally on this node
     * @param packet containing the command
     */
    public void processCommand(NetworkPacket packet) {
        if (packet == null) {
            System.out.println("processCommand: packet eq null");
            return;
        }
        Command command = commandUnpack(packet.getData());

        if (!command.isQuery()) {
            sendCommandToSubSystem(command);
        } else {
            querySubSystem(command, packet.getOrigin());
        }
    }

    private void sendCommandToSubSystem(Command command) {
        switch (command.getType()) {
            case SERVICEHANDLER:
                System.out.println("sendCommandToSubSystem: case: SERVICEHANDLER");
                ServiceHandler serviceHandler = (ServiceHandler) getProvider(StatType.SERVICEHANDLER);
                serviceHandler.processCommand(command);
                break;
            case DISCOVERY:
                System.out.println("sendCommandToSubSystem: case: DISCOVERY");
                ServiceDiscovery serviceDiscovery = (ServiceDiscovery) getProvider(StatType.DISCOVERY);
                serviceDiscovery.processCommand(command);
                break;
            case REQUESTQUEUE:
                System.out.println("sendCommandToSubSystem: case: REQUESTQUEUE");
                RequestQueueManager rqManager = (RequestQueueManager) getProvider(StatType.REQUESTQUEUE);
                rqManager.processCommand(command);
                break;
            default:
                System.out.println("WRONG COMMAND TYPE IN CLASS CONSOLE PROCESSCOMMAND" + command.getType());
                break;
        }
    }

    private void querySubSystem(Command command, Origin origin) {

        InternalRoutingServiceDiscovery internalRouting = (InternalRoutingServiceDiscovery) getProvider(StatType.INTERNALROUTING);
        StatisticsProvider provider = getProvider(command.getType());
        byte[] data;
        try {
            if (provider.getStatType() == StatType.UNKNOWN) {
                data = serializeToBytes(new StatisticsData[0]);
                internalRouting.sendBackToOrigin(origin, PathType.RELAY, data);
                return;
            }
            StatisticsData[] result = provider.processCommand(command);
            if(result == null) {
                data = new byte[0];
            }else if(result.length == 1){
                data = serializeToBytes(result);
            }else{
                data = serializeToBytes(result);
            }
            internalRouting.sendBackToOrigin(origin, PathType.RELAY, data);

            /**
            switch (command.getCommandType()) {
                case STATISTICS_FORSUBSYSTEM:
                    StatisticsData[] statisticsForSubSystem = provider.getStatisticsForSubSystem(command.getName());
                    data = serializeToBytes(statisticsForSubSystem);
                    internalRouting.sendBackToOrigin(origin, PathType.RELAY, data);
                    break;
                case STATISTICS_FORSYSTEM:
                    StatisticsData statistics = provider.getStatistics(command.getTokens());
                    data = serializeToBytes(statistics);
                    internalRouting.sendBackToOrigin(origin, PathType.RELAY, data);
                    break;
                case USAGEHISTORY:
                    break;
                default:
                    break;
            }
             */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a query command to a remote node and gets StatisticsData for that query 
     * @param command the query command that should be sent
     * @param destination the destination for the node
     * @return StatisticsData[] with the result of the query
     * @throws IOException 
     */
    public StatisticsData[] sendQueryCommand(Command command, DestinationMetaData destination) throws IOException {
        StatisticsData[] empty = new StatisticsData[0];
        InternalRoutingServiceDiscovery internalRouting = (InternalRoutingServiceDiscovery) getProvider(StatType.INTERNALROUTING);
        if (internalRouting == null) {
            System.out.println("sendQueryCommand: internalRouting is null");
            return empty;
        }
        command.setQuery(true);
        byte[] data = serializeToBytes(command);
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        ServiceRequest req = internalRouting.sendWithResponse(dest, data, 500);
        if (req == null || req.getData() == null) {
            System.out.println("sendQueryCommand, if req: null");
            return empty;
        }
        byte[] reqData = req.getData(); 
        StatisticsData[] res = null;
        switch (command.getCommandType()) {
            case STATISTICS_FORSUBSYSTEM:
                if(reqData == null){
                    System.out.println("sendQueryCommand, STATISTICS_FORSUBSYSTEM: null" +req.getErrorMessage());
                    return empty;
                }
                if(reqData.length <= 0){
                System.out.println("sendQueryCommand, STATISTICS_FORSUBSYSTEM: length <= 0");
                return empty;
                }
                res = statArrayUnpack(reqData);
                if(res == null){
                    return empty;
                }
                break;
            case STATISTICS_FORSYSTEM:
                res = statArrayUnpack(reqData);
                if(res == null){
                    return empty;
                }
                break;
            case USAGEHISTORY:
                res = statArrayUnpack(reqData);
                if(res == null){
                    return empty;
                }
                break;
            case CHANGE_ACTIVEAMOUNT:
                res = statArrayUnpack(reqData);
                if(res == null){
                    return empty;
                }
                break;
            default:
                System.out.println("Unknown console QueryCommand: " +command.getCommandType());
                return empty;
        }
        return res;
    }

    /**
     * Sends a command to a remote node 
     * @param command the command that should be sent
     * @param destination the destination for the node
     * @throws IOException 
     */
    public void sendCommand(Command command, DestinationMetaData destination) throws IOException {
        InternalRoutingServiceDiscovery internalRouting = (InternalRoutingServiceDiscovery) getProvider(StatType.INTERNALROUTING);
        if (internalRouting == null) {
            return;
        }
        command.setQuery(false);
        byte[] data = serializeToBytes(command);
        DestinationMetaData dest = new DestinationMetaData(destination);
        dest.setPathType(PathType.COMMANDCONTROL);
        internalRouting.sendToDestination(dest, data);
    }
    
    private byte[] serializeToBytes(Serializable data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    private byte[] serializeToBytes(Serializable[] data) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(data);
        return stream.toByteArray();
    }

    private StatisticsData statUnpack(byte[] data) {
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

    private StatisticsData[] statArrayUnpack(byte[] data) {
        if(data == null || data.length <= 0) {
            return  new StatisticsData[0];
        }
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

    private Command commandUnpack(byte[] data) {
        Command command = null;
        try {
            ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(data));
            command = (Command) input.readObject();
        } catch (IOException ex) {
            Logger.getLogger(Console.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(Console.class.getName()).log(Level.SEVERE, null, ex);
        }
        return command;
    }

    private class EmptyProvider implements StatisticsProvider {

        private StatisticsData tempData;

        public EmptyProvider(String message) {
            tempData = new StatisticsData(StatType.UNKNOWN, message, new Integer[0]);
        }

        @Override
        public StatisticsData[] getStatisticsForSubSystem(String name) {
            return new StatisticsData[0];
        }

        @Override
        public StatisticsData getStatistics(String[] name) {
            return new StatisticsData();
        }

        @Override
        public StatisticsProvider getProvider() {
            return this;
        }

        @Override
        public StatType getStatType() {
            return StatType.UNKNOWN;
        }

        @Override
        public StatisticsData[] processCommand(Command command) {
            return new StatisticsData[0];
        }
    }
}
