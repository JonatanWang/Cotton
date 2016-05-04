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

/**
 * The <code>ServiceChain</code> maintains the chain of services to be executed. 
 *
 * @author Magnus
 * @author Jonathan
 * @author Tony
 * @author Gunnlaugur
 */
public interface ServiceChain {
    /**
     * Removes the service name and returns it from the queue.
     * 
     * @return the next service name.
     */
    public String getNextServiceName();
    /**
     * Peeks at the next service name in the queue and returns it. The service 
     * will not be removed from the queue.
     * 
     * @return the next service name.
     */
    public String peekNextServiceName();
    /**
     * Adds a service to the service queue.
     * 
     * @param name the name of the service.
     * @return <code>true</code> if the service is added.
     */
    public boolean addService(String name);
    /**
     * Adds a service to the service queue and returns this object. This allows 
     * more than one add in a single command.
     * 
     * @param name the service to add to the queue.
     * @return <code>this</code> object.
     */
    public ServiceChain into(String name);
}
