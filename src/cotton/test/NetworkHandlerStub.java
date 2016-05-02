package cotton.test;

import cotton.network.NetworkHandler;
import cotton.internalRouting.InternalRoutingNetwork;
import cotton.network.SocketLatch;
import java.net.SocketAddress;
import cotton.network.NetworkPacket;
import java.io.IOException;

/**
*
* @author Tony
*/
public class NetworkHandlerStub implements NetworkHandler{
    private InternalRoutingNetwork internal;
    private SocketAddress addr;
    
    public NetworkHandlerStub(SocketAddress addr){
        this.addr = addr;
        this.internal = null;
    }
    
    /**
     * Sends data wrapped in a <code>NetworkPacket</code> over the network.
     *
     * @param netPacket contains the data and the <code>metadata</code> needed to send the packet.
     * @param addr defines the <code>SocketAddress</code> to send through.
     */
    @Override
    public void send(NetworkPacket netPacket, SocketAddress addr) throws IOException{
        internal.pushNetworkPacket(netPacket);
    }

    /**
     * Sends a <code>NetworkPacket</code> and informs that the connection should stay alive.
     *
     * @param netPacket wraps the keep alive flag.
     * @param addr defines the <code>SocketAddress</code> to send through.
     */
    @Override
    public void sendKeepAlive(NetworkPacket netPacket,SocketAddress addr) throws IOException{
        SocketLatch latch = new SocketLatch();
        internal.pushKeepAlivePacket(netPacket,latch);
    }

    /**
     * Returns the local <code>SocketAddress</code> of the running machine.
     *
     * @return the local <code>SocketAddress</code>.
     */
    @Override
    public SocketAddress getLocalAddress(){
        return addr;
    }

    /**
     * Sets the interface to push data to the rest of the system
     * @param internal this machines routing subsystem
     */
    @Override
    public void setInternalRouting(InternalRoutingNetwork internal){
        this.internal = internal;
    }
    
    /**
     * Asks all connections to shutdown and turns off the <code>NetworkHandler</code>.
     */
    @Override
     public void stop(){}

    @Override
    public void run(){}
}
