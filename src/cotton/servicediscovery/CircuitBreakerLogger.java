/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.servicediscovery;

import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.services.CloudContext;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Magnus
 */
public class CircuitBreakerLogger implements Service {
    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
        CircuitBreakerPacket cb = packetUnpack(data);
        if(cb != null) { 
            System.out.println("A CircuitBreaker was triggerd:" + cb);
        }
        return "none".getBytes();
    }

    public static ServiceFactory getFactory(){
        return new Factory();
    }

    @Override
    public ServiceFactory loadFactory(){
        return new Factory();
    }

    private CircuitBreakerPacket packetUnpack(byte[] data) {        
        CircuitBreakerPacket probe = null;
        try {
            ObjectInputStream input;
            input = new ObjectInputStream(new ByteArrayInputStream(data));
            probe = (CircuitBreakerPacket) input.readObject();
            return probe;
        } catch (IOException | ClassNotFoundException ex) {
            Logger.getLogger(CircuitBreakerLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        return probe;
    }
    
    public static class Factory implements ServiceFactory {
        @Override
        public Service newService() {
            return new CircuitBreakerLogger();
        }
    }
}