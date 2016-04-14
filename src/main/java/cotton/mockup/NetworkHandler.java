/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.cotton.mockup;

import java.io.InputStream;
import java.io.Serializable;

/**
 *
 * @author o_0
 */
public interface NetworkHandler {
    public ServicePacket nextPacket();
    public void sendServiceResult(ServiceConnection from,Serializable result,ServiceChain to);
}
