/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ncotton.etwork;

import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.PathType;
import cotton.network.ServiceChain;

/**
 *
 * @author tony
 * @author magnus
 */
public class DefaultNetworkPacket implements NetworkPacket {
    private Origin origin;
    private ServiceChain serviceChain;
    private PathType pathType;
    private byte[] result;

    public DefaultNetworkPacket(Origin origin, ServiceChain serviceChain, PathType pathType, byte[] result) {
        this.origin = origin;
        this.serviceChain = serviceChain;
        this.pathType = pathType;
        this.result = result;
    }
    
    
    @Override
    public byte[] getData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Origin getOrigin() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public ServiceChain getServiceChain() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public PathType getPathType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setPathType(PathType pathType) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setData(byte[] data) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
