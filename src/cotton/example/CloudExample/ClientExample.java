/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.example.CloudExample;

import cotton.Cotton;
import cotton.internalRouting.InternalRoutingClient;
import cotton.internalRouting.ServiceRequest;
import cotton.network.DummyServiceChain;
import cotton.network.ServiceChain;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

/**
 *
 * @author o_0
 */
public class ClientExample {
    public static void main(String[] args) throws UnknownHostException {
        GlobalDnsStub gDns = getDnsStub("127.0.01", 9546);
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

    private static GlobalDnsStub getDnsStub(String dest, int port) {
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress gdAddr = new InetSocketAddress(dest,port);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
