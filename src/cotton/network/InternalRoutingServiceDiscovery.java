/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.network;

/**
 *
 * @author tony
 */
public interface InternalRoutingServiceDiscovery {
    public boolean SendBackToOrigin(Origin origin,PathType pathType,byte[] data);
    public boolean SendToDestination(DestinationMetaData dest,byte[] data);
    public ServiceRequest sendWithResponse(DestinationMetaData dest, byte[] data);
}
