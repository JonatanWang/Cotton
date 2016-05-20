/*

Copyright (c) 2016, Gunnlaugur Juliusson, Jonathan Kåhre, Magnus Lundmark,
Mats Levin, Tony Tran
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
 * Neither the name of Cotton Production Team nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

 */


package cotton.example.scalingexample;

import cotton.Cotton;
import cotton.configuration.Configurator;
import cotton.internalrouting.ServiceRequest;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;

/**
 *
 * @author Gunnlaugur Juliusson
 * @author Jonathan
 */
public class ScalingClient implements Runnable{
    private AtomicInteger dones;
    
    public ScalingClient(AtomicInteger dones){
        this.dones = dones;
    }
    
    public static void main(String[] args) throws UnknownHostException, MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException, InterruptedException {
        int clientAmount = 1;
        AtomicInteger dones = new AtomicInteger(0);
        long starttime = System.currentTimeMillis();
        
        for(int i = 0; i < clientAmount; i++) 
            new Thread(new ScalingClient(dones)).start();
        while(dones.get() != clientAmount){}
        System.out.println("Time: "+(System.currentTimeMillis()-starttime));
    }
    
    private static JSONObject byteArrayToJson(byte[] data) {
        String convertToJson = new String(data);
        return new JSONObject(convertToJson);
    }
    
    private static byte[] jsonToByteArray(String command) {
        JSONObject j = new JSONObject();
        
        j.put("command", command);
        j.put("data", "data test one");
        j.put("accessLevel", 1);
        
        String data = j.toString();
        
        return data.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void run() {
        int intos = 1;
        int sendAmount = 1000;
        byte[] data;
        Configurator conf;
        Cotton clientInstance = null;
        ServiceRequest serviceRequest = null;
        ServiceChain chain = new DummyServiceChain();;
        
        try {
            conf = new Configurator("Scalingconfig.cfg");
        } catch (Exception ex) {
            conf = new Configurator();
            conf.loadDefaults();
        }
        
        try {
            clientInstance = new Cotton(conf);
        } catch (Exception ex) {}
        
        clientInstance.start();

        data = ByteBuffer.allocate(4).putInt(0, 2).array();
        //data = jsonToByteArray("authoriseRequest");
//        for(int i = 0; i < intos; i++)
//            chain.into("mathpow");
//        clientInstance.getClient().sendToService(data, chain);
        
        //data = jsonToByteArray("getDataFromDatabase");
        for(int i = 1; i < sendAmount+1; i++) {
            for(int j = 0; j < intos; j++)
                chain.into("mathpow");
            serviceRequest = clientInstance.getClient().sendWithResponse(data, chain);
//            try {
//                System.out.println("Number: " + i + " " + byteArrayToJson(serviceRequest.getData()));
//            } catch(Exception e) {}
        }
        
//        for(int i = 0; i < intos; i++)
//                chain.into("mathpow");
//        //data = jsonToByteArray("removeDataFromDatabase");
//        clientInstance.getClient().sendToService(data, chain);

        System.out.println("Done: " + dones.incrementAndGet());
        
        clientInstance.shutdown();
    }
}
