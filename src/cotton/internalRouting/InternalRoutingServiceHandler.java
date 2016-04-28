/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package internalRouting;

import cotton.network.Origin;
import cotton.network.ServiceChain;

/**
 *
 * @author tony
 */
public interface InternalRoutingServiceHandler {
    public boolean forwardResult(Origin origin, ServiceChain serviceChain, byte[] result);
}
