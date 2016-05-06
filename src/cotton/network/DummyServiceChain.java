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
package cotton.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The <code>DummyServiceChain</code> implements the <code>ServiceChain</code>
 * interface and maintains the chain of services to be executed.
 *
 * @author Magnus
 * @see ServiceChain
 */
public class DummyServiceChain implements ServiceChain, Serializable {

    private static final long serialVersionUID = 1L;
    private ConcurrentLinkedQueue<String> chain;

    /**
     * Constructs an empty <code>ServiceChain</code> with a concurrent linked
     * queue.
     */
    public DummyServiceChain() {
        this.chain = new ConcurrentLinkedQueue<>();
    }

    /**
     * Constructs a <code>ServiceChain</code> with a given service name. The
     * <code>ServiceChain</code> is constructed with a concurrent linked queue.
     *
     * @param serviceName the service name to add to the
     * <code>serviceChain</code>.
     */
    public DummyServiceChain(String serviceName) {
        this.chain = new ConcurrentLinkedQueue<>();
        chain.add(serviceName);
    }

    private DummyServiceChain(ServiceChainBuilder builder) {
        this.chain = new ConcurrentLinkedQueue<>(builder.bChain);
    }

    /**
     * Adds a service name into the <code>ServiceChain</code>.
     *
     * @param name the service name.
     * @return <code>true</code> if successful.
     */
    @Override
    public boolean addService(String name) {
        chain.add(name);
        return true;
    }

    /**
     * Removes the service name and returns it from the queue.
     *
     * @return the next service name.
     */
    @Override
    public String getNextServiceName() {
        return chain.poll();
    }

    /**
     * Peeks at the next service name in the queue and returns it. The service
     * will not be removed from the queue.
     *
     * @return the next service name.
     */
    @Override
    public String peekNextServiceName() {
        return chain.peek();
    }

    /**
     * Adds a service to the service queue and returns this object. This allows
     * more than one add in a single command.
     *
     * @param name the service to add to the queue.
     * @return <code>this</code> object.
     */
    @Override
    public ServiceChain into(String name) {
        chain.add(name);
        return this;
    }

    /**
     * Returns a <code>String</code> containing the different service names.
     *
     * @return the different service names.
     */
    @Override
    public String toString() {
        return chain.toString();
    }

    public static class ServiceChainBuilder {
        private ArrayList<String> bChain = null;

        public ServiceChainBuilder() {
            this.bChain = new ArrayList<String>();
        }
        
        public DummyServiceChain build() {
            return new DummyServiceChain(this);
        }

        public ServiceChainBuilder into(String name) {
            bChain.add(name);
            return this;
        }
    }
}
