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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author Tony
 * @author Magnus
 */
public class UsageHistory implements Serializable{
    //private Vector<TimeInterval> usageHistoryList;
    private List<TimeInterval> usageHistoryList;
    
    public UsageHistory(){
        this.usageHistoryList = Collections.synchronizedList(new ArrayList<TimeInterval>());
    }
    /**
     * Adds a time interval to the historyList
     * @param element 
     */
    public void add(TimeInterval element){
        usageHistoryList.add(element);
    }
    
    public void clearHistory() {
        this.usageHistoryList.clear();
    }
    
    /**
     * returns the usage history for the request queue.
     * @return 
     */
    public TimeInterval[] getUsageHistory(){
        TimeInterval[] tmp = usageHistoryList.toArray(new TimeInterval[usageHistoryList.size()]);
        return tmp;
    }
    /**
     * Returns a specific time interval if the lower bound is less than 0 
     * or upper bound is greater than max then the entire usage history list will be returned.
     * @param first
     * @param last
     * @return 
     */
    public TimeInterval[] getInterval(int first, int last){  
        int lastIndex = (last >= usageHistoryList.size()) ? usageHistoryList.size() : last;
        int firstIndex = (first <= 0) ? 0 : first;
        firstIndex = (firstIndex >= usageHistoryList.size()) ? usageHistoryList.size() : firstIndex;
        lastIndex = (firstIndex >= lastIndex) ? usageHistoryList.size() : lastIndex;
        ArrayList<TimeInterval> tmp = new ArrayList<>(usageHistoryList.subList(firstIndex, lastIndex));
        TimeInterval[] ret = new TimeInterval[tmp.size()];
        tmp.toArray(ret);
        return ret;
    }
    /**
     * returns the total amount of time intervals in the usage history. 
     * @return 
     */
    
    public int getLastIndex(){
        return usageHistoryList.size();
    }
}
