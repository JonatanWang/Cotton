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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The <code>AESEncryption</code> consists of encrypt and decrypt methods as 
 * well as a <code>key</code> setter. Before use the <strong>AES</strong> key 
 * and <code>initVector</code> must be set.
 * 
 * @author Gunnlaugur Juliusson
 * @see Encryption
 */
public class AESEncryption implements Encryption{
    private String key;
    private String initVector;
    
    /**
     * Constructs a <code>AESEncryption</code> without setting the <code>key</code> or 
     * the <code>initVector</code>.
     */
    public AESEncryption() {
        key = null;
        initVector = null;
    }
    
    /**
     * Encrypts the data with <strong>AES</strong> and returns the result.
     * 
     * @param data the data to be encrypted.
     * @return the encrypted data.
     */
    @Override
    public byte[] encryptData(byte[] data) throws UnsupportedEncodingException,
            NoSuchAlgorithmException, 
            NoSuchPaddingException,
            InvalidKeyException, 
            InvalidAlgorithmParameterException, 
            IllegalBlockSizeException, 
            BadPaddingException 
    {
        if(key == null) throw new NullPointerException("key: null");
        if(initVector == null) throw new NullPointerException("initVector: null");
        
        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);

        return cipher.doFinal(data);
    }

    /**
     * Decrypts the data with <strong>AES</strong> and returns the result.
     * 
     * @param data the data to be decrypted.
     * @return the decrypted data.
     */
    @Override
    public byte[] decryptData(byte[] data) throws InvalidKeyException, 
            InvalidAlgorithmParameterException,
            NoSuchAlgorithmException,
            UnsupportedEncodingException, 
            NoSuchPaddingException, 
            IllegalBlockSizeException, 
            BadPaddingException 
    {
        if(key == null) throw new NullPointerException("key: null");
        if(initVector == null) throw new NullPointerException("initVector: null");
        
        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);

        return cipher.doFinal(data);
    }

    /**
     * Sets the <strong>AES</strong> key. 
     * The key is required to be a multiple of 16.
     * 
     * @param key the <strong>AES</strong> key to be set.
     */
    @Override
    public void setKey(byte[] key) throws IOException, ClassNotFoundException{
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(key));
        String keyAES = (String) input.readObject();
        this.key = keyAES;
        input.close();
    }
    
    /**
     * Sets the <code>initVector</code>. 
     * The initVector is required to be a multiple of 16.
     * 
     * @param initVector the <strong>AES</strong> vector to be set.
     */
    public void setInitVector(String initVector) {
        if(initVector == null) throw new NullPointerException("initVector null");
        
        this.initVector = initVector;
    }
}
