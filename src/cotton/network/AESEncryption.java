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
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class AESEncryption implements Encryption{
    private String key;
    private String initVector;
    
    @Override
    public byte[] encryptData(byte[] data) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            
            byte[] enc = cipher.doFinal(data);

            return enc;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        // Should not return data that is not encrypted.
        return null;
    }

    @Override
    public byte[] decryptData(byte[] data) {
        byte[] dec = null;
        
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes("UTF-8"), "AES");
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            
            dec = cipher.doFinal(data);

            return dec;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        return null;
    }

    /**
     * Sets the <strong>AES</strong> key. 
     * The key is required to be a multiple of 16.
     * 
     * @param key the <strong>AES</strong> key to be set.
     */
    @Override
    public void setKey(byte[] key) {
        ObjectInputStream input = null;
        String keyAES = null;
        
        try {
            input = new ObjectInputStream(new ByteArrayInputStream(key));
            keyAES = (String) input.readObject();
            this.key = keyAES;
        } catch (IOException ex) {
            try {
                throw ex;
            } catch (IOException ex1) {}
        } catch (ClassNotFoundException ex) {
            try {
                throw ex;
            } catch (ClassNotFoundException ex1) {}
        } finally {
            try {
                input.close();
            } catch (IOException ex) {}
        }
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
