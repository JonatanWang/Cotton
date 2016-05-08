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
import java.util.Arrays;
/**
 * 
 * @author Magnus ,Tony
 */
public class Console {
    private ArrayList<StatisticsProvider> subSystems;
    /**
     * 
     * @param serviceDiscovery
     * @param queueManager
     * @param serviceHandler 
     */
    public Console(StatisticsProvider[] subSystems) {
        this.subSystems = new ArrayList<>();
        this.subSystems.addAll(Arrays.asList(subSystems));
    }
    
    public Console(ArrayList<StatisticsProvider> subSystems) {
        this.subSystems = subSystems;
    }
    
    public Console() {
        this.subSystems = new ArrayList<>();
    }

    public void addSubSystem(StatisticsProvider system){
        this.subSystems.add(system);
    }
    
    public StatisticsProvider getProvider(StatType type){
        for(StatisticsProvider p : subSystems){
            if(p.getStatType() == type){
                return p;
            }
        }
        return new EmptyProvider("Unknown provider: " + type.toString());
    }
    
    private class EmptyProvider implements StatisticsProvider{
        private StatisticsData tempData;
        public EmptyProvider(String message){
            tempData = new StatisticsData(StatType.UNKNOWN,message,new Integer[0]);
        }
        
        @Override
        public StatisticsData[] getStatisticsForSubSystem(String name) {
            return new StatisticsData[0];
        }

        @Override
        public StatisticsData getStatistics(String[] name) {
            return new StatisticsData();
        }

        @Override
        public StatisticsProvider getProvider() {
            return this;
        }

        @Override
        public StatType getStatType() {
            return StatType.UNKNOWN;
        }
        
    }
}
