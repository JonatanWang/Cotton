/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.network;

import cotton.services.DefaultServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

/**
 *
 * @author o_0
 */
public class DefaultNetworkHandler implements NetworkHandler {
    private ServiceBuffer serviceBuffer;
    public DefaultNetworkHandler() {
        this.serviceBuffer = new DefaultServiceBuffer();
        
    }
    
    @Override
    public ServicePacket nextPacket() {
        return serviceBuffer.nextPacket();
    }

    @Override
    public void sendServiceResult(ServiceConnection from, Serializable result, ServiceChain to) {
        
        if(to.getCurrentServiceName() != null){
            DummyBufferStuffer bufferStuffer = new DummyBufferStuffer(from,result,to);
            bufferStuffer.fillBuffer();
        }
        
    }
    
    private class DummyBufferStuffer{
        private ServicePacket servicePacket;
        public DummyBufferStuffer(ServiceConnection from, Serializable data, ServiceChain to){
            try{
                PipedInputStream in = new PipedInputStream();
                PipedOutputStream outStream = new PipedOutputStream(in);
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                objectOutStream.writeObject(data);
                objectOutStream.close();
            
                this.servicePacket = new ServicePacket(from,in,to);
            }catch(IOException e){
                e.printStackTrace();
            }
        }

        public void fillBuffer(){
            serviceBuffer.add(this.servicePacket);
        }
        

    }
    
}
