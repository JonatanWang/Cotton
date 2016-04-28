/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.network;
import cotton.network.PathType;
import java.net.SocketAddress;

/**
 *
 * @author tony
 */
public class DestinationMetaData {
    private SocketAddress socketAddress;
    private PathType pathType;

    /**
  	* Default empty DestinationMetaData constructor
  	*/
  	public DestinationMetaData() {
      socketAddress = null;
      pathType = PathType.NOTFOUND;
  	}

    /**
    *
    *
    **/
    public DestinationMetaData(SocketAddress socketAddress, PathType pathType) {
        this.socketAddress = socketAddress;
        this.pathType = pathType;
    }


    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public PathType getPathType() {
        return pathType;
    }

    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }

}
