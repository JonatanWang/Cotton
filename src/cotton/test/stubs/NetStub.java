/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test.stubs;

import cotton.network.NetworkPacket;
import java.io.IOException;
import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author o_0
 */
public class NetStub {

    private ConcurrentHashMap<SocketAddress, NetworkHandlerStub> tubes = new ConcurrentHashMap<>();
    public void addNode(NetworkHandlerStub net) {
        SocketAddress addr = net.getLocalAddress();
        if(addr == null) {
            return;
        }
        this.tubes.putIfAbsent(addr, net);
    }
    
    public void removeNode(NetworkHandlerStub net) {
        SocketAddress addr = net.getLocalAddress();
        if(addr == null) {
            return;
        }
        this.tubes.remove(addr, net);
    }
    
    public void forwardKeepAlive(NetworkPacket netPacket, SocketAddress addr) throws IOException {
        NetworkHandlerStub get = tubes.get(addr);
        if (get == null) {
            throw new IOException("Connection failure");
        }
        get.recvKeepAlive(netPacket);
    }

    public void forwardSend(NetworkPacket netPacket, SocketAddress addr) throws IOException {
        NetworkHandlerStub get = tubes.get(addr);
        if (get == null) {
            throw new IOException("Connection failure");
        }
        get.recv(netPacket);
    }
}
