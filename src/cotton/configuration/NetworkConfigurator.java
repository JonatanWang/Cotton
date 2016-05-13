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

package cotton.configuration;

import java.net.InetSocketAddress;

/**
 * Configurator for the network handler
 *
 * @author Jonathan Kåhre
 */
public class NetworkConfigurator {
    private InetSocketAddress address = null;
    private boolean encryption = false;
    private String keystore = null;
    private String password = null;

    private  NetworkConfigurator (InetSocketAddress address, boolean encryption, String keystore, String password) {
        this.address = address;
        this.encryption = encryption;
        this.keystore = keystore;
        this.password = password;
    }

    /**
     * Returns the configured address
     *
     * @return The pre-configured address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Returns if encryption is enabled for this configurator
     *
     * @return The encryption configuration
     */
    public boolean isEncryptionEnabled() {
        return encryption;
    }

    /**
     * Returns the configured name of the keystore
     *
     * @return The pre-configured keystore
     */
    public String getKeystore() {
        return keystore;
    }

    /**
     * Returns the configured keystore-password
     *
     * @return The pre-configured keystore-password
     */
    public String getPassword() {
        return password;//TODO: Encrypt password
    }

    @Override
    public String toString() {
        return "NetworkConfigurator{" +
                "address=" + address +
                ", encryption=" + encryption +
                ", keystore='" + keystore + '\'' +
                '}';
    }

    /**
     * Builder for the network configurator
     */
    public static final class Builder {
        private InetSocketAddress address;
        private boolean encryption;
        private String keystore;
        private String password;

        public Builder setAddress(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        public Builder setEncryption(boolean encryption) {
            this.encryption = encryption;
            return this;
        }

        public Builder setKeystore(String keystore) {
            this.keystore = keystore;
            return this;
        }

        public Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public NetworkConfigurator build() {
            return new NetworkConfigurator(address, encryption, keystore, password);
        }
    }
}
