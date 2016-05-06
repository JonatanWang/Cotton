/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.example.cloudexample;

import cotton.Cotton;
import java.net.UnknownHostException;

/**
 *
 * @author o_0
 */
public class DiscoveryExample {
    public static void main(String[] args) throws UnknownHostException {
        Cotton discovery = new Cotton(true, 9546);
        

        discovery.start();
        try {
            Thread.sleep(40000);
        } catch (InterruptedException ex) {
            //Logger.getLogger(UnitTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        discovery.shutdown();
    }
}
