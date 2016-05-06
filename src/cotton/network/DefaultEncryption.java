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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The <code>DefaultEncryption</code> consists of encrypt and decrypt methods as 
 * well as a <strong>RSA</strong> keyAES generator. Before use the <strong>RSA</strong> 
 * and <strong>AES</strong> keys must be set.
 * 
 * @author Gunnlaugur Juliusson
 * @see Encryption
 */
public class DefaultEncryption implements Encryption{
    private String keyAES;
    private String initVector;
    private KeyPair kp;
    
    /**
     * Encrypts the data through <strong>AES</strong> and then through <strong>RSA</strong>.
     * 
     * @param data the data to be encrypted.
     * @return the encrypted data.
     */
    @Override
    public byte[] encryptData(byte[] data) {
        byte[] enc = encryptDataAES(data);
        
        return encryptDataRSA(enc);
    }

    /**
     * Decrypts the data through <strong>RSA</strong> and then through <strong>AES</strong>.
     * 
     * @param data the data to be decrypted.
     * @return the decrypted data.
     */
    @Override
    public byte[] decryptData(byte[] data) {
        byte[] dec = decryptDataRSA(data);
        
        return decryptDataAES(dec);
    }
    
    /**
     * Sets the <strong>RSA</strong> <code>KeyPair</code> as <code>key</code>.
     * 
     * @param key the <strong>RSA</strong> key.
     */
    @Override
    public void setKey(byte[] key) {
        ObjectInputStream input = null;
        
        try {
            KeyPair keyPair = null;
            input = new ObjectInputStream(new ByteArrayInputStream(key));
            keyPair = (KeyPair) input.readObject();
            this.kp = keyPair;
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
     * Sets the <strong>AES</strong> key and then the initialization vector. 
     * Both the key and the vector are required to be multiples of 16.
     * 
     * @param key the <strong>AES</strong> key to be set.
     * @param initVector the <strong>AES</strong> vector to be set.
     */
    public void setAESKey(String key, String initVector) {
        if(key == null) throw new NullPointerException("AES Key: null");
        if(initVector == null) throw new NullPointerException("InitVector: null");
        
        //must be multiple of 16
        this.keyAES = key;
        this.initVector = initVector;
    }
    
    private byte[] encryptDataAES(byte[] data) {
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec keySpec = new SecretKeySpec(keyAES.getBytes("UTF-8"), "AES");
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            
            byte[] enc = cipher.doFinal(data);
            
            System.out.println("AES Encrypted data: " + Arrays.toString(enc));
            
            return enc;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        // Should not return data that is not encrypted.
        return null;
    }

    private byte[] decryptDataAES(byte[] data) {
        byte[] dec = null;
        
        try {
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec keySpec = new SecretKeySpec(keyAES.getBytes("UTF-8"), "AES");
            
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            
            dec = cipher.doFinal(data);
            
            System.out.println("AES Decrypted data: " + Arrays.toString(dec));
            
            return dec;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        return data;
    }
    
    /**
     * Generates the public and private <strong>RSA</strong> keys with a 
     * byte size of 512.
     * 
     * @throws java.security.NoSuchAlgorithmException <code>KeyPair</code> generation failed. 
     */
    public void generateRSAKeys() throws NoSuchAlgorithmException {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(4096);
            kp = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException ex) {
            throw ex;
        }
    }
    
    private byte[] encryptDataRSA(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, kp.getPublic());
            
            byte[] enc = cipher.doFinal(data);

            System.out.println("RSA Encrypted data: " + Arrays.toString(enc));
            
            return enc;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        return null;
    }

    private byte[] decryptDataRSA(byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, kp.getPrivate());
            
            byte[] dec = cipher.doFinal(data);

            System.out.println("RSA Decrypted data: " + Arrays.toString(dec));
            
            return dec;
        } catch(Exception e) {
            System.out.println(e.toString());
        } //TODO Fix
        
        return data;
    }
}
