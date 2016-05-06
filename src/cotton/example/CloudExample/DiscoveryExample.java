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
    public static void main(String[] args) {
        Cotton discovery = null;
        try {
            discovery = new Cotton(true, 9546);
        } catch (UnknownHostException ex) {
            //Logger.getLogger(DiscoveryExample.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        discovery.start();
        try {
            Thread.sleep(90000);
        } catch (InterruptedException ex) {
        }
        discovery.shutdown();
    }
}
