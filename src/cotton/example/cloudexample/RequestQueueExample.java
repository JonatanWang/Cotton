/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.example.cloudexample;

import cotton.Cotton;
import cotton.requestqueue.RequestQueueManager;
import cotton.test.services.GlobalDnsStub;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author o_0
 */
public class RequestQueueExample {
    public static void main(String[] args) throws UnknownHostException {
        GlobalDnsStub gDns = getDnsStub("127.0.01", 9546);
        Cotton queueInstance = new Cotton(false, gDns);
        RequestQueueManager requestQueueManager = new RequestQueueManager();
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
    
    private static GlobalDnsStub getDnsStub(String dest, int port) {
        GlobalDnsStub gDns = new GlobalDnsStub();
        InetSocketAddress gdAddr = new InetSocketAddress(dest,port);
        InetSocketAddress[] arr = new InetSocketAddress[1];
        arr[0] = gdAddr;
        gDns.setGlobalDiscoveryAddress(arr);
        return gDns;
    }
}
