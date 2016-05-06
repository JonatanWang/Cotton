/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.example.CloudExample;

import cotton.Cotton;
import cotton.test.UnitTest;
import cotton.test.services.GlobalDnsStub;
import cotton.test.services.MathPowV2;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author o_0
 */
public class ServiceExample2 {
    public static void main(String[] args) throws UnknownHostException {
        GlobalDnsStub gDns = getDnsStub("127.0.0.1", 9546);
        Cotton cotton = new Cotton(false, gDns);
        cotton.getServiceRegistation().registerService("mathpow21", MathPowV2.getFactory(), 10);
        cotton.start();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException ex) {
            Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
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
