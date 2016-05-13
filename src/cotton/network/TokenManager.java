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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * The <code>TokenManager</code> manages the encryption and decryption of 
 * <code>Tokens</code>. Before use the <strong>RSA</strong> <code>KeyPair</code> 
 * must be set.
 * 
 * @author Gunnlaugur Juliusson
 */
public class TokenManager {
    private final RSAEncryption rsa;
    private boolean keySet;
    
    /**
     * Constructs a <code>TokenManager</code> without setting the encryption keys.
     */
    public TokenManager() {
        rsa = new RSAEncryption();
        keySet = false;
    }
    
    /**
     * Generates a <code>KeyPair</code> based on the <code>RSAEncryption</code> 
     * function and sets it as the <code>TokenManager</code> key.
     * 
     * @throws IOException if <code>key</code> conversion fails.
     * @throws ClassNotFoundException if <code>key</code> conversion fails.
     * @throws NoSuchAlgorithmException if the algorithm does not exist.
     */
    public void setKey() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        KeyPair kp = rsa.generateRSAKeys();
        rsa.setKey(keyPairToByteArray(kp));
        
        keySet = true;
    }
    
    /**
     * Sets the <code>TokenManager</code> key as the given parameter.
     * 
     * @param kp the key to be set.
     * @throws IOException if <code>key</code> conversion fails.
     * @throws ClassNotFoundException if <code>key</code> conversion fails.
     * @throws NoSuchAlgorithmException if the algorithm does not exist.
     */
    public void setKey(KeyPair kp) throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
        rsa.setKey(keyPairToByteArray(kp));
        
        keySet = true;
    }
    
    /**
     * Encrypts the <code>Token</code> with <strong>RSA</strong> and returns it 
     * as a <code>byte</code> array.
     * 
     * @param t the <code>Token</code> to be encrypted.
     * 
     * @return the encrypted <code>Token</code> as a <code>byte</code> array.
     * 
     * @throws IOException if the <code>Token</code> cast fails.
     * @throws IllegalBlockSizeException if invalid block size.
     * @throws BadPaddingException if padding is bad.
     * @throws InvalidKeyException if <code>key</code> is invalid.
     * @throws NoSuchAlgorithmException if the algorithm does not exist.
     * @throws NoSuchPaddingException if padding does not exist.
     */
    public byte[] encryptToken(Token t) throws IOException, 
            IllegalBlockSizeException, 
            BadPaddingException, 
            InvalidKeyException, 
            NoSuchAlgorithmException, 
            NoSuchPaddingException 
    {
        if(keySet == false) throw new UnsupportedOperationException("KeyPair not set!");
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(t);
        
        byte[] tokenAsBytes = stream.toByteArray();
        
        return rsa.encryptData(tokenAsBytes);
    }
    
    /**
     * Decrypts the token with <strong>RSA</strong> and returns it 
     * as a <code>Token</code>.
     * 
     * @param token the token to decrypt.
     * 
     * @return the decrypted <code>Token</code>.
     * 
     * @throws IOException if the <code>Token</code> cast fails.
     * @throws ClassNotFoundException if <code>class</code> does not exist.
     * @throws IllegalBlockSizeException if invalid block size.
     * @throws BadPaddingException if padding is bad.
     * @throws InvalidKeyException if <code>key</code> is invalid.
     * @throws NoSuchAlgorithmException if the algorithm does not exist.
     * @throws NoSuchPaddingException if padding does not exist.
     */
    public Token decryptToken(byte[] token) throws IOException, 
            ClassNotFoundException, 
            IllegalBlockSizeException, 
            BadPaddingException, 
            InvalidKeyException, 
            NoSuchAlgorithmException, 
            NoSuchPaddingException 
    {
        if(keySet == false) throw new UnsupportedOperationException("KeyPair not set!");
        
        ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(rsa.decryptData(token)));
        Token dToken = (Token) input.readObject();
        input.close();
        
        return dToken;
    }
    
    private byte[] keyPairToByteArray(KeyPair kp) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(kp);
        
        return stream.toByteArray();
    }
}
