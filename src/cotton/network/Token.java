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
import java.util.Date;

/**
 * The <code>Token class</code> acts as a security authentication token.
 * 
 * @author Gunnlaugur Juliusson
 */
public class Token implements Serializable{
    private final int secLevel;
    private final Date timeStamp;
    private final String name;
    private final String id;

    /**
     * Constructs a authentication token with the given parameters. As the 
     * parameters are set they cannot be changed.
     * 
     * @param secLevel the security level.
     * @param name the username.
     * @param id the user id.
     */
    public Token(int secLevel, String name, String id) {
        if(secLevel < 0) throw new UnsupportedOperationException("secLevel: null");
        if(name == null) throw new NullPointerException("name: null");
        if(id == null) throw new NullPointerException("id: null");
        
        this.timeStamp = new Date();
        this.secLevel = secLevel;
        this.name = name;
        this.id = id;
    }
    
    /**
     * Returns the users security level in the cloud.
     * 
     * @return the security level.
     */
    public int getSecLevel() {
        return secLevel;
    }

    /**
     * Returns the time when the token was created.
     * 
     * @return time of creation.
     */
    public Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * Returns the username in the token.
     * 
     * @return the username.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the user <code>id</code> in the token.
     * 
     * @return the id.
     */
    public String getId() {
        return id;
    }
    
    /**
     * Returns a string containing all the <code>Token</code> information.
     * 
     * @return the <code>Token</code> information.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("secLevel: ").append(secLevel).append(", ");
        sb.append("timeStamp: ").append(timeStamp.toString()).append(", ");
        sb.append("name: ").append(name).append(", ");
        sb.append("id: ").append(id);
        
        return sb.toString();
    }
}
