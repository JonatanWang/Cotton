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

import cotton.network.PathType;
import java.io.Serializable;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.util.Objects;

/**
 *
 * @author tony
 */
public class DestinationMetaData implements Serializable {

    private SocketAddress socketAddress;
    private PathType pathType;

    /**
     * Default empty DestinationMetaData constructor
     */
    public DestinationMetaData() {
        socketAddress = null;
        pathType = PathType.NOTFOUND;
    }

    /**
     * This will create a new DestinationMetaData from a old one
     * @param old the one that should be copy
     */
    public DestinationMetaData(DestinationMetaData old) {
        socketAddress = old.socketAddress;
        pathType = old.pathType;
    }
    /**
     *
     *
     *
     */
    public DestinationMetaData(SocketAddress socketAddress, PathType pathType) {
        this.socketAddress = socketAddress;
        this.pathType = pathType;
    }

    public SocketAddress getSocketAddress() {
        return socketAddress;
    }

    public void setSocketAddress(SocketAddress socketAddress) {
        this.socketAddress = socketAddress;
    }

    public PathType getPathType() {
        return pathType;
    }

    public void setPathType(PathType pathType) {
        this.pathType = pathType;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.socketAddress);
        hash = 97 * hash + Objects.hashCode(this.pathType);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DestinationMetaData other = (DestinationMetaData) obj;
        if (!Objects.equals(this.socketAddress, other.socketAddress)) {
            return false;
        }
        if (this.pathType != other.pathType) {
            return false;
        }
        return true;
    }

    public boolean compareAddress(SocketAddress other) {
        return ((InetSocketAddress) socketAddress).equals((InetSocketAddress) other);
    }

    @Override
    public String toString() {
        return "DestinationMetaData{" + "socketAddress=" + (InetSocketAddress) socketAddress + ", pathType=" + pathType + '}';
    }

}
