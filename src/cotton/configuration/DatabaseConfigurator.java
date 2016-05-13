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

/**
 * Configurator for the database.
 *
 * @author Jonathan Kåhre
 */
public class DatabaseConfigurator{
    private String backend;
    private String address;
    private int port;
    private String databaseName;

    private DatabaseConfigurator(String backend, String address, int port, String databasename) {
        this.backend = backend;
        this.address = address;
        this.databaseName = databasename;
        this.port = port;
    }

    /**
     * Returns the configured backend
     *
     * @return The pre-configured backend
     */
    public String getBackend() {
        return backend;
    }

    /**
     * Returns the configured address
     *
     * @return The pre-configured address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the configured port
     *
     * @return The pre-configured port
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the configured database name
     *
     * @return The pre-configured database name
     */
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String toString() {
        return "DatabaseConfigurator{" +
                "backend='" + backend + '\'' +
                ", address='" + address + '\'' +
                ", port='" + port + '\'' +
                ", databasename='" + databaseName + '\'' +
                '}';
    }

    /**
     * Builder for the database configurator
     */
    public static final class Builder {
        private String backend;
        private String address;
        private String databasename;
        private int port;

        public Builder setBackend(String backend) {
            this.backend = backend;
            return this;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder setDatabasename(String databasename) {
            this.databasename = databasename;
            return this;
        }

        public Builder setPort(int port){
            if(port > 65535 || port < 1)
                throw new MalformedConfigurationException("Invalid port: "+port);
            this.port = port;
            return this;
        }

        public DatabaseConfigurator build() {
            return new DatabaseConfigurator(backend, address, port, databasename);
        }
    }

}
