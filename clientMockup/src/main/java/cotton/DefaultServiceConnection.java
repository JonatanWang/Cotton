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

import java.net.InetAddress;
import java.net.SocketAddress;
import java.util.Random;
import java.io.Serializable;

import java.util.UUID;

/**
 *
 * @author Magnus
 */
public class DefaultServiceConnection implements ServiceConnection, Serializable {
    private static final long serialVersionUID = 1L;
    private UUID conId;
    private String name;
    private PathType pathType = PathType.SERVICE;
    SocketAddress address = null;

    public DefaultServiceConnection() {
        conId = UUID.randomUUID();
        this.name = "none";
    }

    public DefaultServiceConnection(UUID uuid) {
        conId = uuid;
        this.name = "none";
    }

    public DefaultServiceConnection(String name) {
        conId = UUID.randomUUID();
        this.name = name;
    }

    @Override
    public UUID getUserConnectionId() {
        return this.conId;
    }
/**
 *  Will be deprecated soon.. security vector
 * @param uuid set the uiid for this connection,
 */
    public void setUserConnectionId(UUID uuid) {
        this.conId = uuid;
    }
    
    @Override
    public String getServiceName() {
        return this.name;
    }

    @Override
    public SocketAddress getAddress() {
        return this.address;
    }

    @Override
    public void setAddress(SocketAddress addr) {
        this.address = addr;
    }

    @Override
    public PathType getPathType() {
        return this.pathType;
    }

    @Override
    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }
    
}
