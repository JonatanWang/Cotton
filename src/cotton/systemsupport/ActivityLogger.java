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
package cotton.systemsupport;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Magnus
 */
public class ActivityLogger {

    private UsageHistory usageHistory;
    private long samplingRate = 0;
    private Timer timeManger = new Timer();
    private TimeSliceTask sliceTask = null;
    private AtomicBoolean running = new AtomicBoolean(false);

    public ActivityLogger(long samplingRate) {
        this.samplingRate = samplingRate;
        this.usageHistory = new UsageHistory();
    }

    
    /**
     * Start the Usage History recording and sets the sampling rate
     *
     * @param samplingRate
     * @return
     */
    public boolean setUsageRecording(long samplingRate) {
        this.samplingRate = (int) samplingRate;
        if (this.sliceTask != null) {
            sliceTask.cancel();

        }
        this.sliceTask = new TimeSliceTask(System.currentTimeMillis());
        timeManger.scheduleAtFixedRate(sliceTask, 0, this.samplingRate);
        return true;
    }

    public long getSamplingRate() {
        return this.samplingRate;
    }
    
    /**
     * get the Usage History recording from start to end
     *
     * @param samplingRate
     * @return
     */
    public TimeInterval[] getUsageRecording(int start,int end) {
        if(end == 0) {
            end = this.usageHistory.getLastIndex();
        }
        return this.usageHistory.getInterval(start, end);
    }
    
    /**
     * Stop the Usage History recording
     *
     * @return
     */
    public boolean stopUsageRecording() {
        if (sliceTask != null) {
            sliceTask.cancel();
        }
        return true;
    }

    public boolean hasRunningTimer() {
        if (timeManger == null) {
            return false;
        }
        return true;
    }

    public void stop() {
        running.set(false);
        if (this.sliceTask != null) {
            this.sliceTask.cancel();
        }
        timeManger.cancel();
        timeManger.purge();
        timeManger = null;
    }

    private AtomicInteger inputCounter = new AtomicInteger(0);
    private AtomicInteger outputCounter = new AtomicInteger(0);
    private AtomicInteger currentActiveCount = new AtomicInteger(0);
    public int getLastIndex() {
        return this.usageHistory.getLastIndex();
    }
    
    public void clearHistory() {
        this.usageHistory.clearHistory();
    }
    
    public void setCurrentActiveCount(int count) {
        this.currentActiveCount.set(count);
    }
    
    public void recordInputEvent() {
        this.inputCounter.incrementAndGet();
    }
    
    public void recordOutputEvent() {
        this.outputCounter.incrementAndGet();
    }
    
    private class TimeSliceTask extends TimerTask {

        private long startTime;

        public TimeSliceTask(long startTime) {
            this.startTime = startTime;
        }

        @Override
        public void run() {
            long endTime = System.currentTimeMillis();
            long deltaTime = endTime - startTime;
            int in = inputCounter.getAndSet(0);
            int out = outputCounter.getAndSet(0);
            
            TimeInterval timeInterval = new TimeInterval(deltaTime);
            timeInterval.setCurrentActiveCount(currentActiveCount.get());
            timeInterval.setInputCount(in);
            timeInterval.setOutputCount(out);
            usageHistory.add(timeInterval);
            startTime = System.currentTimeMillis();
        }
    }

}
