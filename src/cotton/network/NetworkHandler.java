package cotton.network;

import cotton.internalRouting.InternalRoutingNetwork;
import java.io.IOException;
import java.net.SocketAddress;

/**
 * Implementing this interface enables a <code>class</code> to act as a 
 * network handler. The <code>NetworkHandler</code> manages the network traffic 
 * by creating <code>SocketAddress</code> connections and sending data.
 * <p>
 * Each <code>Cotton</code> instance needs a <code>NetworkHandler</code> to pass
 * traffic between nodes.
 * 
 * @author tony
 */
public interface NetworkHandler extends Runnable {
    /**
     * Sends data wrapped in a <code>NetworkPacket</code> over the network. 
     * 
     * @param netPacket contains the data and the <code>metadata</code> needed to send the packet.
     * @param dest defines the <code>SocketAddress</code> to send through.
     * @throws java.io.IOException
     */
    public void send(NetworkPacket netPacket, SocketAddress dest) throws IOException;
  
    /**
     * Sends a <code>NetworkPacket</code> and informs that the connection should stay alive.
     * 
     * @param netPacket wraps the keep alive flag.
     * @param dest defines the <code>SocketAddress</code> to send through.
     * @throws java.io.IOException
     */
    public void sendKeepAlive(NetworkPacket netPacket,SocketAddress dest) throws IOException;
    
    /**
     * Returns the local <code>SocketAddress</code> of the running machine.
     * 
     * @return the local <code>SocketAddress</code>.
     */
    public SocketAddress getLocalAddress();
    
    /**
     * Sets the interface to push data to the rest of the system
     * @param internal this machines routing subsystem
     */
    public void setInternalRouting(InternalRoutingNetwork internal);
    /**
     * Asks all connections to shutdown and turns off the <code>NetworkHandler</code>.
     */
    public void stop();
}
