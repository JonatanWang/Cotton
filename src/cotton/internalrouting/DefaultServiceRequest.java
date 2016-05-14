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

package cotton.internalrouting;

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author tony
 */
public class DefaultServiceRequest implements ServiceRequest{
    private byte[] data = null;
    private CountDownLatch latch = new CountDownLatch(1);
    private long timeStamp = 0;
    private String errorMessage;

    public DefaultServiceRequest(){
        
    }

    public DefaultServiceRequest(long timeStamp){
        this.timeStamp = timeStamp;
    }

    public byte[] getData() {
        boolean loop = false;
        do {
            try {
                latch.await();
                loop = false;
            } catch (InterruptedException ex) {loop = true;}
        }while(loop);
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
        latch.countDown();
    }

    public void setFailed(String errorMessage) {
        if(data == null){
            this.errorMessage = errorMessage;
            latch.countDown();
        }
    }

    public long getTimeStamp(){
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp){
        this.timeStamp = timeStamp;
    }

    /**
     * This method returns an error message if the fail has triggered data equals null
     * @return errorMessage  
     */
    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
