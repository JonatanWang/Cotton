package cotton.network;

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
     * @param addr defines the <code>SocketAddress</code> to send through.
     * @return <code>true</code> if the send was successful.
     */
    public boolean send(NetworkPacket netPacket, SocketAddress addr);
  
    /**
     * Sends a <code>NetworkPacket</code> and informs that the connection should stay alive.
     * 
     * @param netPacket wraps the keep alive flag.
     * @param addr defines the <code>SocketAddress</code> to send through.
     * @return the latch for the connection.
     */
    public boolean sendKeepAlive(NetworkPacket netPacket,SocketAddress addr);
    
    /**
     * Returns the local <code>SocketAddress</code> of the running machine.
     * 
     * @return the local <code>SocketAddress</code>.
     */
    public SocketAddress getLocalAddress();
    
    /**
     * Asks all connections to shutdown and turns off the <code>NetworkHandler</code>.
     */
    public void stop();
}
