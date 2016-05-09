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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.crypto.Cipher;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class RSAEncryption implements Encryption {
    private KeyPair keyPair;
    
    @Override
    public byte[] encryptData(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            
            byte[] enc = cipher.doFinal(data);

            return enc;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        return null;
    }

    @Override
    public byte[] decryptData(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            
            byte[] dec = cipher.doFinal(data);
            
            return dec;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        return data;
    }

    /**
     * Sets the <strong>RSA</strong> <code>KeyPair</code> as <code>key</code>.
     * 
     * @param key the <strong>RSA</strong> key.
     */
    @Override
    public void setKey(byte[] key) {
        ObjectInputStream input = null;
        KeyPair keyPair = null;
        
        try {
            input = new ObjectInputStream(new ByteArrayInputStream(key));
            keyPair = (KeyPair) input.readObject();
            this.keyPair = keyPair;
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
     * Generates the public and private <strong>RSA</strong> keys with a 
     * byte size of 512.
     * 
     * @return 
     * @throws java.security.NoSuchAlgorithmException <code>KeyPair</code> generation failed. 
     */
    public KeyPair generateRSAKeys() throws NoSuchAlgorithmException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            return kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw ex;
        }
    }
}
