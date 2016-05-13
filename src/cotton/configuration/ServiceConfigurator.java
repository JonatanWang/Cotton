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

import java.util.ArrayList;

/**
 * Configurator for the queue
 *
 * @author Jonathan Kåhre
 */
public class ServiceConfigurator{
    private ArrayList<ServiceData> services;
    private int threadLimit;

    public ServiceConfigurator(ArrayList<ServiceData> services, int threadLimit) {
        this.services = services;
        this.threadLimit = threadLimit;
    }

    /**
     * Returns the list of configured services
     *
     * @return The pre-configured list of services
     */
    public ArrayList<ServiceData> getServices() {
        return (ArrayList<ServiceData>) services.clone();
    }

    /**
     * Returns true if there are services to load.
     *
     * @return Whether there are services to load or not.
     */
    public boolean hasServices() {
        return services == null || !services.isEmpty();
    }

    /**
     * The limit of threads to run
     *
     * @return The pre-configured thread-limit
     */
    public int getThreadLimit() {
        return threadLimit;
    }

    @Override
    public String toString() {
        return "ServiceConfigurator{" +
                "services=" + services +
                ", threadLimit=" + threadLimit +
                '}';
    }

    /**
     * Consolidation class for service-metadata
     */
    public static final class ServiceData{
        private String name;
        private int limit;

        public ServiceData(String name, int limit) {
            this.name = name;
            this.limit = limit;
        }

        public String getName() {
            return name;
        }

        public int getLimit() {
            return limit;
        }

        @Override
        public String toString(){
            return "ServiceData["+
                "name="+name+
                ",limit="+limit+
                "]";
        }
    }

    /**
     * Builder for the service configurator
     */
    public static final class Builder {
        private ArrayList<ServiceData> services;
        private int threadLimit;

        public Builder(){
            services = new ArrayList<>();
        }

        public void addService(String serviceName, int threadLimit) {
            this.services.add(new ServiceData(serviceName, threadLimit));
        }

        public void setThreadLimit(int threadLimit) {
            this.threadLimit = threadLimit;
        }

        public ServiceConfigurator build(){
            return new ServiceConfigurator(services, threadLimit);
        }
    }
}
