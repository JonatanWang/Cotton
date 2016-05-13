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


package cotton.test;

import cotton.network.AESEncryption;
import cotton.network.RSAEncryption;
import cotton.network.Token;
import cotton.network.TokenManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Gunnlaugur Juliusson
 */
public class TestEncryption {
    
    @Test
    public void TestEncryption() throws NoSuchAlgorithmException, 
            IOException, 
            ClassNotFoundException, 
            UnsupportedEncodingException, 
            NoSuchPaddingException, 
            InvalidKeyException, 
            InvalidAlgorithmParameterException, 
            IllegalBlockSizeException, 
            BadPaddingException 
    {
        RSAEncryption rsa = new RSAEncryption();
        AESEncryption aes = new AESEncryption();

        aes.setKey(stringToByteArray("Cotton1234Cloud5"));
        aes.setInitVector("CottonInitVector");
        rsa.setKey(keyPairToByteArray(rsa));
        
        byte[] data = {0,1,2,3,4};
        System.out.println("Initial data: " + Arrays.toString(data));

        byte[] enc = aes.encryptData(data);
        System.out.println("AES Encrypted data: " + Arrays.toString(enc));
        
        enc = rsa.encryptData(enc);
        System.out.println("RSA Encrypted data: " + Arrays.toString(enc));
        
        byte[] dec = rsa.decryptData(enc);
        System.out.println("RSA Decrypted data: " + Arrays.toString(dec));
        
        dec = aes.decryptData(dec);
        System.out.println("AES Decrypted data: " + Arrays.toString(dec));
        
        assertTrue(Arrays.equals(data, dec));
    }
    
    @Test
    public void TestTokenManager() throws IOException, 
            ClassNotFoundException, 
            NoSuchAlgorithmException, 
            IllegalBlockSizeException, 
            BadPaddingException, 
            InvalidKeyException, 
            NoSuchPaddingException 
    {
        TokenManager tm = new TokenManager();
        tm.setKey();
        
        Token t = new Token(4, "user", "123");
        System.out.println("Initial token:\n" + t.toString());
        
        byte[] encryptedToken = tm.encryptToken(t);
        System.out.println("\nToken as bytes: " + Arrays.toString(encryptedToken));
        
        t = tm.decryptToken(encryptedToken);
        System.out.println(t.toString());
    }

    private byte[] stringToByteArray(String s) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(s);
        return stream.toByteArray();
    }
    
    private byte[] keyPairToByteArray(RSAEncryption rsa) throws IOException, NoSuchAlgorithmException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(stream);
        objectStream.writeObject(rsa.generateRSAKeys());
        return stream.toByteArray();
    }
}