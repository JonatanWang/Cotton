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

package cotton.example;

import cotton.network.Origin;
import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class FileWriterService implements Service{

    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
        System.out.println("Write");
        BufferedImage image = null;

        ImageManipulationPacket input = null;
        try{
            //input = (ImageManipulationPacket)new ObjectInputStream(data).readObject();
            //image = bytesToBufferedImage(input.getImage());
            image = bytesToBufferedImage(data);

            ImageIO.write(image, "jpg", new File("test.jpg"));
        }catch (IOException ex) {
            Logger.getLogger(ImageManipulationService.class.getName()).log(Level.SEVERE, null, ex);
        }

        return data;
    }

    public static ServiceFactory getFactory(){
        return new FileFactory();
    }

    @Override
    public ServiceFactory loadFactory(){
        return new FileFactory();
    }

    public static class FileFactory implements ServiceFactory {

        @Override
        public Service newService() {
            return new FileWriterService();
        }

    }

    private BufferedImage bytesToBufferedImage(byte[] serializedImage){
        InputStream in = new ByteArrayInputStream(serializedImage);
        BufferedImage image = null;
        try {
            image = ImageIO.read(in);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        return image;
    }

    private byte[] bufferedImageToBytes(BufferedImage image){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", output);
        }
        catch (Throwable e) {
            System.out.println("Error " + e.getMessage());
            e.printStackTrace();
        }
        return output.toByteArray();
    }
}
