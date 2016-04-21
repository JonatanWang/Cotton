
package cotton.servicediscovery;

import java.io.Serializable;
import java.net.SocketAddress;

/**
 *
 * @author Magnus
 */
public class DiscoveryProbe implements Serializable {
        private String name;
        private SocketAddress address;

        public DiscoveryProbe(String name, SocketAddress address) {
            this.name = name;
            this.address = address;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public SocketAddress getAddress() {
            return address;
        }

        public void setAddress(SocketAddress address) {
            this.address = address;
        }
        
    }
