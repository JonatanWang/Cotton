/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.internalRouting;

import cotton.network.DestinationMetaData;
import cotton.network.ServiceChain;
import cotton.internalRouting.ServiceRequest;

/**
 *
 * @author tony
 */
public interface InternalRoutingLogger {
    public boolean sendToNextService(byte[] data, ServiceChain chain);
    public boolean loggerToDestination(DestinationMetaData dest, byte[] data);
    public ServiceRequest loggerWithResponse(DestinationMetaData dest, byte[] data);
}
