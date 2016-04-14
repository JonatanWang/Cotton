package cotton.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import cotton.services.DefaultServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceChain;
import cotton.services.ServiceConnection;
import cotton.services.ServicePacket;

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
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);


                oos.writeObject(data);

                oos.flush();
                oos.close();

                InputStream in = new ByteArrayInputStream(baos.toByteArray());
                /*
                System.out.println("1");
                PipedOutputStream outStream = new PipedOutputStream();
                System.out.println("2");
                PipedInputStream in = new PipedInputStream(outStream);
                System.out.println("3");
                ObjectOutputStream objectOutStream = new ObjectOutputStream(outStream);
                System.out.println("4");
                objectOutStream.writeObject(data);
                System.out.println("5");
                objectOutStream.flush();
                objectOutStream.close();*/

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
