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
import cotton.internalrouting.InternalRoutingClient;
import cotton.internalrouting.ServiceRequest;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import cotton.test.services.GlobalDnsStub;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONObject;

/**
 *
 * @author Gunnlaugur
 */
public class DBClient {
    public static void main(String[] args) throws UnknownHostException, MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        Configurator conf;
        try {
            conf = new Configurator("DBClientconfig.cfg");
        } catch (Exception ex) {
            conf = new Configurator();
            conf.loadDefaults();
        }
        
        
        Cotton clientInstance = new Cotton(conf);
        
        clientInstance.start();
        
        InternalRoutingClient clientNetwork = clientInstance.getClient();
        ServiceChain chain = new DummyServiceChain().into("database");
        ServiceRequest serviceRequest = null;
        
        byte[] data = jsonToByteArray("authoriseRequest");
        clientNetwork.sendToService(data, chain);
        
        data = jsonToByteArray("getDataFromDatabase");
        serviceRequest = clientNetwork.sendWithResponse(data, chain);
        
        data = jsonToByteArray("removeDataFromDatabase");
        clientNetwork.sendToService(data, chain);
        System.out.println("Waiting for response");
        if(serviceRequest != null) {
            data = serviceRequest.getData();
            JSONObject j = byteArrayToJson(data);
            
            System.out.println("this is the result: " + j.toString());
        }else {
            System.out.println("Failed to send");
        }
        
        clientInstance.shutdown();
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
}
