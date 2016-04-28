/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.net.SocketAddress;
import cotton.network.NetworkPacket;
import cotton.network.SocketLatch;

/**
 *
 * @author tony
 */
public interface NetworkHandler extends Runnable {
    public boolean send(NetworkPacket netPacket, SocketAddress addr);
    public SocketLatch sendKeepAlive(NetworkPacket netPacket,SocketAddress addr);
    public SocketAddress getLocalAddress();
    public void stop();

}
