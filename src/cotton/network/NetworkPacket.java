/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.network;

/**
 *
 * @author tony
 * @author Magnus
 */
public interface NetworkPacket {
    public byte[] getData();
    public Origin getOrigin();
    public ServiceChain getServiceChain();
    public PathType getPathType();
    public void setPathType(PathType pathType);
    public void setData(byte[] data);
}
