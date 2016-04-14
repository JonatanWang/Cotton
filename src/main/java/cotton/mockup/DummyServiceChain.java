/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.cotton.mockup;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 * @author o_0
 */
public class DummyServiceChain implements ServiceChain {
    private ConcurrentLinkedQueue<String> chain;
    public DummyServiceChain(String serviceName) {
        this.chain = new ConcurrentLinkedQueue<String>();
        chain.add(serviceName);
        
    }

    @Override
    public boolean addService(String name) {
        chain.add(name);
        return true;
    }

    @Override
    public String getNextServiceName() {
        return chain.poll();
    }

    @Override
    public String getCurrentServiceName() {
        return chain.peek();
    }
    
}
