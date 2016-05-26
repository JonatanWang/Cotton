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


package cotton.example.scalingexample;

import cotton.Cotton;
import cotton.configuration.Configurator;
import cotton.services.ActiveServiceLookup;
import cotton.services.ServiceFactory;
import cotton.storagecomponents.DatabaseService;
import cotton.test.services.MathPowV2;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 *
 * @author Gunnlaugur
 */
public class SHRunning implements Runnable{
    public static void main(String[] args) throws UnknownHostException, MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        int clientAmount = 2;
        
        for(int i = 0; i < clientAmount; i++) {
            Thread th = new Thread(new SHRunning());
            th.setDaemon(true);
            th.start();
        }
        Scanner scan = new Scanner(System.in);
        boolean run = true;
        while(run) {
            try {
                if(Integer.parseInt(scan.nextLine()) == 1)
                    run = false;
            } catch(Exception e) {}
        }
    }

    @Override
    public void run() {
        Configurator conf;
        try {
            conf = new Configurator("SHconfig.cfg");
        } catch (Exception ex) {
            conf = new Configurator();
            conf.loadDefaults();
        }
        
        Cotton shInstance = null;
        try {
            shInstance = new Cotton(conf);
        } catch (Exception ex) {}
        
        ActiveServiceLookup asl = shInstance.getServiceRegistation();
        ServiceFactory sf = DatabaseService.getFactory(); 
        //asl.registerService("database", sf, 10);
        sf = MathPowV2.getFactory();
        asl.registerService("mathpow", sf, 10); 
        
        shInstance.start();
        
        Scanner scan = new Scanner(System.in);
        boolean run = true;
        while(run) {
            try {
                if(Integer.parseInt(scan.nextLine()) == 1)
                    run = false;
            } catch(Exception e) {}
        }
        
        shInstance.shutdown();
    }
}
