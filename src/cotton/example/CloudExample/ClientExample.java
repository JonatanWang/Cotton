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
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.ServiceRequest;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 *
 * @author Magnus
 */
public class ClientExample {

    public static void main(String[] args) throws UnknownHostException {
        GlobalDnsStub gDns = getDnsStub(null, 9546);
        Cotton cotton = new Cotton(false, gDns);
        cotton.start();

        InternalRoutingClient client = cotton.getClient();
        ServiceChain chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");

        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();

        //ServiceRequest req = client.sendWithResponse(data, chain);
        for (int i = 0; i < 1000; i++) {
            chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21").into("result");
            client.sendToService(data, chain);
        }

        chain = new DummyServiceChain().into("mathpow2").into("mathpow21").into("mathpow2").into("mathpow21");
        ServiceRequest req = client.sendWithResponse(data, chain);
        data = req.getData();
        num = ByteBuffer.wrap(data).getInt();
        System.out.println("result:  : " + num);
        cotton.shutdown();
    }

    private static GlobalDnsStub getDnsStub(String dest, int port) throws UnknownHostException {
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress gdAddr = null;
        if (dest == null) {
            gdAddr = new InetSocketAddress(Inet4Address.getLocalHost(), port);
            System.out.println("discAddr:" + Inet4Address.getLocalHost().toString() +" port: " + port);
        } else {
            gdAddr = new InetSocketAddress(dest, port);
        }
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
