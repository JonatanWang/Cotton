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
package cotton.example.cloudexample;

import cotton.Cotton;
import cotton.configuration.Configurator;
import cotton.requestqueue.RequestQueueManager;
import cotton.test.services.GlobalDnsStub;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

/**
 *
 * @author Magnus
 */
public class RequestQueueExample {

    public static void main(String[] args) throws UnknownHostException, IOException {
        GlobalDnsStub gDns = getDnsStub(null, 9546);
        Cotton queueInstance = new Cotton(false, gDns);
        Configurator config = new Configurator();
        config.loadConfigFromFile("configurationtemplate.cfg");
        RequestQueueManager requestQueueManager = new RequestQueueManager(config.getQueueConfigurator());
        requestQueueManager.startQueue("mathpow21");
        requestQueueManager.startQueue("mathpow2");
        queueInstance.setRequestQueueManager(requestQueueManager);
        queueInstance.start();
        try {
            Thread.sleep(80000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        queueInstance.shutdown();
    }

    private static GlobalDnsStub getDnsStub(String dest, int port) throws UnknownHostException {
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress gdAddr = null;
        if (dest == null) {
            gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
            System.out.println("discAddr:" + Inet4Address.getLocalHost().toString() +" port: " + port);
        }else {
            gdAddr = new InetSocketAddress(dest, port);
        }
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
