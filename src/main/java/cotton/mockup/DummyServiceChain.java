/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main.java.cotton.mockup;

/**
 *
 * @author o_0
 */
public class DummyServiceChain implements ServiceChain {
    private String serviceName;

    public DummyServiceChain(String serviceName) {
        this.serviceName = serviceName;
    }
    
    @Override
    public String getFirstServiceName() {
        return this.serviceName;
    }

    @Override
    public boolean addService(String name) {
        this.serviceName = name;
        return true;
    }
    
}
