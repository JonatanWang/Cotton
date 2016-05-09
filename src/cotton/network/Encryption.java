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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Implementing this interface enables a <code>class</code> to act as a 
 * Encryption handler. The Encryption handler manages both encryption and 
 * decryption for <code>byte arrays</code>.
 * 
 * @author Gunnlaugur Juliusson
 */
public interface Encryption {
    /**
     * Encrypts a <code>byte array</code> and returns it.
     * 
     * @param data the data to encrypt.
     * 
     * @return the encrypted result.
     * 
     * @throws javax.crypto.BadPaddingException if padding is bad.
     * @throws javax.crypto.IllegalBlockSizeException if invalid block size.
     * @throws java.security.InvalidAlgorithmParameterException if algorithm parameter is invalid.
     * @throws java.security.InvalidKeyException if <code>key</code> is invalid.
     * @throws java.security.NoSuchAlgorithmException if algorithm does not exist.
     * @throws javax.crypto.NoSuchPaddingException if padding does not exist.
     * @throws java.io.UnsupportedEncodingException if encoding is unsupported.
     */
    public byte[] encryptData(byte[] data) throws BadPaddingException,
            IllegalBlockSizeException, 
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            NoSuchAlgorithmException, 
            NoSuchPaddingException,
            UnsupportedEncodingException;
    
    /**
     * Decrypts a <code>byte array</code> and returns it.
     * 
     * @param data the data to decrypt.
     * 
     * @return the decrypted result.
     * 
     * @throws javax.crypto.BadPaddingException if padding is bad.
     * @throws javax.crypto.IllegalBlockSizeException if invalid block size.
     * @throws java.security.InvalidAlgorithmParameterException if algorithm parameter is invalid.
     * @throws java.security.InvalidKeyException if <code>key</code> is invalid.
     * @throws java.security.NoSuchAlgorithmException if algorithm does not exist.
     * @throws javax.crypto.NoSuchPaddingException if padding does not exist.
     * @throws java.io.UnsupportedEncodingException if encoding is unsupported.
     */
    public byte[] decryptData(byte[] data) throws BadPaddingException,
            IllegalBlockSizeException,
            InvalidAlgorithmParameterException,
            InvalidKeyException,
            NoSuchAlgorithmException,
            NoSuchPaddingException,
            UnsupportedEncodingException;
    
    /**
     * Sets the encryption key for the encryption handler.
     * 
     * @param key the encryption key.
     * 
     * @throws java.io.IOException if <code>key</code> conversion fails.
     * @throws java.lang.ClassNotFoundException if <code>key</code> conversion fails.
     */
    public void setKey(byte[] key) throws ClassNotFoundException, IOException;
}
