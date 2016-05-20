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
package cotton.servicediscovery;

import java.io.Serializable;

/**
 * The <code>DiscoveryPacket</code> acts as a wrapper for the
 * <code>DiscoveryProbe</code> and the <code>AnnouncePacket</code>. Each packet
 * consists of a <code>DiscoveryPacketType</code> used to determine the packet
 * purpose.
 *
 * @author Mats
 * @author Magnus
 * @see DiscoveryProbe
 * @see AnnouncePacket
 */
public class DiscoveryPacket implements Serializable {

    private DiscoveryProbe probe = null;
    private AnnouncePacket announce = null;
    private DiscoveryPacketType type;
    private QueuePacket queue;
    private TopologyPacket topology;
    private ConfigurationPacket configPacket;
    private CircuitBreakerPacket circuitBreakerPacket;
    
    
    public enum DiscoveryPacketType {
        DISCOVERYREQUEST, DISCOVERYRESPONSE, ANNOUNCE, REQUESTQUEUE, TOPOLOGY, CONFIG, CIRCUITBREAKER
    }

    /**
     * Returns the <code>QueuePacket</code> connected to the
     * <code>DiscoveryPacket</code>.
     *
     * @return the connected <code>QueuePacket</code>.
     */
    public QueuePacket getQueue() {
        return queue;
    }

    /**
     * Sets the containing <code>QueuePacket</code> to the incoming packet.
     *
     * @param queue new <code>QueuePacket</code>.
     */
    public void setQueue(QueuePacket queue) {
        this.queue = queue;
    }

    /**
     * Returns the <code>ConfigurationPacket</code> connected to the
     * <code>DiscoveryPacket</code>.
     *
     * @return the connected <code>ConfigurationPacket</code>.
     */
    public ConfigurationPacket getConfigPacket() {
        return configPacket;
    }

    /**
     * Sets the containing <code>ConfigurationPacket</code> to the incoming
     * packet.
     *
     * @param configPacket new <code>ConfigurationPacket</code>.
     */
    public void setConfigPacket(ConfigurationPacket configPacket) {
        this.configPacket = configPacket;
    }

    /**
     * Constructs an empty <code>DiscoveryPacket</code> consisting only of the
     * <code>DiscoveryPacketType</code>.
     *
     * @param type the packet type.
     */
    public DiscoveryPacket(DiscoveryPacketType type) {
        this.type = type;
    }

    /**
     * Returns the purpose of the <code>DiscoveryPacket</code> represented as a
     * <code>DiscoveryPacketType</code>.
     *
     * @return the <code>DiscoveryPacketType</code>.
     */
    public DiscoveryPacketType getPacketType() {
        return type;
    }

    /**
     * Changes the <code>DiscoveryPacketType</code> through the parameter.
     *
     * @param type the new <code>DiscoveryPacketType</code>.
     */
    public void setPacketType(DiscoveryPacketType type) {
        this.type = type;
    }

    /**
     * Returns the <code>AnnouncePacket</code> connected to the
     * <code>DiscoveryPacket</code>.
     *
     * @return the connected <code>AnnouncePacket</code>.
     */
    public AnnouncePacket getAnnounce() {
        return announce;
    }

    /**
     * Sets the containing <code>AnnouncePacket</code> to the incoming packet.
     *
     * @param announce new <code>AnnouncePacket</code>.
     */
    public void setAnnonce(AnnouncePacket announce) {
        this.announce = announce;
    }

    /**
     * Returns the <code>DiscoveryProbe</code> connected to the
     * <code>DiscoveryPacket</code>.
     *
     * @return the connected <code>DiscoveryProbe</code>.
     */
    public DiscoveryProbe getProbe() {
        return probe;
    }

    /**
     * Sets the containing <code>DiscoveryProbe</code> to the incoming probe.
     *
     * @param probe new <code>AnnounceProbe</code>.
     */
    public void setProbe(DiscoveryProbe probe) {
        this.probe = probe;
    }

    public TopologyPacket getTopology() {
        return topology;
    }

    public void setTopologyPacket(TopologyPacket topology) {
        this.topology = topology;
    }

    public CircuitBreakerPacket getCircuitBreakerPacket() {
        return circuitBreakerPacket;
    }

    public void setCircuitBreakerPacket(CircuitBreakerPacket circuitBreakerPacket) {
        this.circuitBreakerPacket = circuitBreakerPacket;
    }

    @Override
    public String toString() {
        return "DiscoveryPacket{" +
                "probe=" + probe +
                ", announce=" + announce +
                ", type=" + type +
                ", queue=" + queue +
                ", topology=" + topology +
                ", configPacket=" + configPacket +
                ", circuitBreakerPacket=" + circuitBreakerPacket +
                '}';
    }
}
