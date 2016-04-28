/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.net.SocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author tony
 */
public interface NetworkHandler extends Runnable {
    public boolean send(cotton.network.NetworkPacket netPacket, SocketAddress addr);
    public CountDownLatch sendKeepAlive(cotton.network.NetworkPacket netPacket,SocketAddress addr);
    public void stop();

}
