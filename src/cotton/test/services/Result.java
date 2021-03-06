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
package cotton.test.services;

import cotton.network.Origin;
import cotton.services.CloudContext;
import cotton.network.ServiceChain;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import cotton.services.Service;
import cotton.services.ServiceFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Magnus
 */
public class Result implements Service {

    public AtomicInteger resCount = new AtomicInteger(0);
    private int dotFreq = 0;
    private int lineFreq = 0;

    public Result(AtomicInteger counter, int dotFreq, int lineFreq) {
        this.resCount = counter;
        this.dotFreq = dotFreq;
        this.lineFreq = lineFreq;
    }

    @Override
    public byte[] execute(CloudContext ctx, Origin origin, byte[] data, ServiceChain to) {
        int num = 0;
        num = ByteBuffer.wrap(data).getInt();
        int count = resCount.incrementAndGet();
        if (count % dotFreq == 0) {
            System.out.print(".");
        }
        if (count % lineFreq == 0) {
            System.out.println(",");
        }
        return data;
    }

    public static ServiceFactory getFactory(AtomicInteger counter, int dotFreq, int lineFreq) {
        return new Factory(counter,dotFreq,lineFreq);
    }

    @Override
    public ServiceFactory loadFactory() {
        return new Factory(new AtomicInteger(0),1,100);
    }

    public static class Factory implements ServiceFactory {

        private AtomicInteger resCount;
        private int dotFreq = 0;
        private int lineFreq = 0;

        public Factory(AtomicInteger counter, int dotFreq, int lineFreq) {
            this.resCount = counter;
            this.dotFreq = dotFreq;
            this.lineFreq = lineFreq;
        }

        public AtomicInteger getCounter() {
            return resCount;
        }

        @Override
        public Service newService() {
            return new Result(resCount,dotFreq,lineFreq);
        }
    }
}
