/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test;

import cotton.Cotton;
import cotton.internalrouting.InternalRoutingServiceHandler;
import cotton.network.DummyServiceChain.ServiceChainBuilder;
import cotton.network.NetworkPacket;
import cotton.network.Origin;
import cotton.network.ServiceChain;
import cotton.services.ActiveServiceLookup;
import cotton.services.BridgeServiceBuffer;
import cotton.services.ServiceBuffer;
import cotton.services.ServiceHandler;
import cotton.services.ServiceLookup;
import cotton.test.services.MathPowV2;
import cotton.test.services.MathResult;
import cotton.test.services.Result;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author o_0
 */
public class TestStressTolerance {

    public TestStressTolerance() {
    }

    @Test
    public void TestServiceHandlerFlood() throws UnknownHostException {
        int sentChains = 1000000;
        System.out.println("Now running: TestServiceHandlerFlood: " + sentChains + " chains test");
        AtomicInteger counter = new AtomicInteger(0);
        int dotFreq = 10000;
        int lineFreq = 100000;
        Result.Factory resFactory = (Result.Factory) Result.getFactory(counter,dotFreq,lineFreq);

        ActiveServiceLookup serviceLookup = new ServiceLookup();
        serviceLookup.registerService("mathpow2", MathPowV2.getFactory(), 20);
        serviceLookup.registerService("mathpow21", MathPowV2.getFactory(), 20);
        serviceLookup.registerService("result", resFactory, 100);

        InternalRoutingServiceHandler internal = new InternalRoutingServiceHandler() {
            private BridgeServiceBuffer bridge = new BridgeServiceBuffer();

            @Override
            public boolean forwardResult(Origin origin, ServiceChain serviceChain, byte[] result) {
                if (serviceChain.peekNextServiceName() == null) {
                    return true;
                }
                bridge.add(NetworkPacket
                        .newBuilder()
                        .setOrigin(origin)
                        .setPath(serviceChain)
                        .setData(result)
                        .build());
                return true;
            }

            @Override
            public ServiceBuffer getServiceBuffer() {
                return bridge;
            }

            @Override
            public boolean notifyRequestQueue(String serviceName) {
                return true;
            }
        };

        ServiceHandler serviceHandler = new ServiceHandler(serviceLookup, internal);
        Thread th = new Thread(serviceHandler);
        ServiceBuffer serviceBuffer = internal.getServiceBuffer();
        ServiceChainBuilder build = new ServiceChainBuilder();
        build.into("mathpow2")
                .into("mathpow21").into("mathpow2")
                .into("mathpow2").into("mathpow21")
                .into("mathpow2").into("mathpow21").into("result");
        int num = 2;
        byte[] data = ByteBuffer.allocate(4).putInt(num).array();
        
        for (int i = 0; i < sentChains; i++) {
            NetworkPacket pkt = NetworkPacket.newBuilder()
                    .setOrigin(new Origin())
                    .setPath(build.build())
                    .setData(data)
                    .build();
            serviceBuffer.add(pkt);
        }
        System.out.println("Service buffer loaded with: " + serviceBuffer.size() + " packets");
        th.start();
        int completedChains = counter.get();
        int timeOut = 0;
        int maxRunTime = 1000;
        double ratio = 1.0/100.0;
        int waitTime = (int) ((double)maxRunTime*ratio);
        int loop = maxRunTime - waitTime;
        System.out.println("MaxRunTime: " + maxRunTime + " ratio: " + ratio + " loop: " + loop + " waitTime: " + waitTime);
        long startTime = System.currentTimeMillis();
        while (completedChains < sentChains && timeOut < loop) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException ex) {}
            timeOut++;
        }
        long stopTime = System.currentTimeMillis();
        completedChains = counter.get();
        float total = (float)(stopTime - startTime)/1000.0f;
        System.out.println("Chains completed: " + completedChains + " in: " + total +"[s] , " + (float)completedChains/total + "[chains/sec]" );
        serviceHandler.stop();
        assertTrue(sentChains == completedChains);
    }
}
