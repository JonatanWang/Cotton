/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cotton.test.services;

import cotton.servicediscovery.GlobalDiscoveryDNS;
import java.net.SocketAddress;

/**
 *
 * @author o_0
 */
public class GlobalDnsStub extends GlobalDiscoveryDNS {

        private SocketAddress[] addressArray = null;

        @Override
        public void setGlobalDiscoveryAddress(SocketAddress[] addresses) {
            this.addressArray = addresses;
        }

        @Override
        public SocketAddress[] getGlobalDiscoveryAddress() {
            return this.addressArray;
        }
    }