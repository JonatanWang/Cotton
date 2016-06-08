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
package Controller;


import cotton.network.DestinationMetaData;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.StatisticsRecorder.SampleRange;
import java.util.ArrayList;
import javafx.stage.Stage;
import model.CloudStat;
import view.DataPusherGraph;
import view.MainView;

/**
 *
 * @author Magnus
 * @author Mats
 */
public class LoggerController {
    private MainView view;
    private Stage primaryStage;
    private CloudStat cloudStat;

    public LoggerController(MainView view,CloudStat cloudStat, Stage primaryStage) {
        this.view = view;
        this.cloudStat = cloudStat;
        this.primaryStage = primaryStage;
        this.cloudStat.setDelegate(this);
        this.view.setDelegate(this);
    }
    
    public boolean connectCloud(String ip,int port) {
        return this.cloudStat.resetCloudLink(ip, port);
    }
    
    public void disconnectCloud() {
        this.cloudStat.shutDown();
    }
    
    public ArrayList<StatisticsData<DestinationMetaData>> getNodesFor(StatType type) {
        return this.cloudStat.getNodes(type);
    }
    
    public void requestUsageData(String name, DataPusherGraph graph, DestinationMetaData dest,SampleRange range) {
        this.cloudStat.getStatData(name, graph, dest,range);
    }
    
    
    public void showScene(Stage rootStage) {
        this.view.showScene(rootStage);
    }
    
}
