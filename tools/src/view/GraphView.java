package view;

import Controller.LoggerController;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.systemsupport.StatType;
import cotton.systemsupport.TimeInterval;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import javafx.animation.AnimationTimer;
import javafx.collections.ObservableList;
import javafx.scene.chart.Chart;
//import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

/**
 *
 * @author Magnus
 * @author Mats
 */
public class GraphView implements DataPusherGraph<TimeInterval> {

    //private AreaChart<Number, Number> graph;
    private LineChart<Number, Number> graph;
    private ConcurrentHashMap<String, PlotInterval> plotData;
    private String chartName;
    private String dataName;

    private class PlotInterval {

        XYChart.Series in;
        XYChart.Series out;
        long startTime = 0;
        int maxPoint = 20;
        String myName;

        public PlotInterval(String name) {
            this.myName = name;
            this.in = new XYChart.Series();
            this.out = new XYChart.Series();
            this.in.setName(name + " input");
            this.out.setName(name + " output");
        }

        public void setMaxPoint(int p) {
            this.maxPoint = p;
        }

        public int getMaxPoint(int p) {
            return this.maxPoint;
        }

        public XYChart.Series getIn() {
            return in;
        }

        public XYChart.Series getOut() {
            return out;
        }

        public void addTimeInterval(TimeInterval[] data) {
            //startTime = 0;
//            this.in.getData().clear();
//            this.out.getData().clear();
            ObservableList data1 = this.in.getData();
            
            for (int i = 0; i < data.length; i++) {
                TimeInterval d = data[i];
                this.in.getData().add(new XYChart.Data(startTime, d.calculateInputIntensity()));
                this.out.getData().add(new XYChart.Data(startTime, d.calculateOutputIntensity()));
                startTime += 1; //d.getDeltaTime();
            }
            TimeInterval[] sampling = data;
            System.out.println("Sampling:" + this.myName);

            System.out.println("Sample count: " + sampling.length);
            for (int i = 0; i < sampling.length; i++) {
                System.out.println("\t" + sampling[i].toString());
            }

            if (this.in.getData().size() > this.maxPoint) {
                this.in.getData().remove(0, this.in.getData().size() - this.maxPoint - 1);
                xA.setLowerBound(0);
                System.out.println("In; rsize: " + this.out.getData().size());
            }

            if (this.out.getData().size() > this.maxPoint) {
                this.out.getData().remove(0, this.out.getData().size() - this.maxPoint - 1);
                xA.setLowerBound(0);
                System.out.println("out; rsize: " + this.out.getData().size());
            }
            if(startTime > 20) {
                startTime = 0;
            }
            xA.setUpperBound(20);
//            xA.setLowerBound(startTime - this.maxPoint);
            System.out.println("start time: " + startTime + " size: in: "+this.in.getData().size()+"out:" +this.out.getData().size());
            //xA.setAutoRanging(true);
            //this.out.getChart().setLayoutX(startTime);

        }

    }

    public class UpdateTimer extends AnimationTimer {

        private long lastTime = 0;
        private long diff = 0;

        @Override
        public void handle(long now) {
            displayData();
            diff += now - lastTime;
            lastTime = now;
            if (diff > 1000000000) {
                System.out.println("Tick: " + diff / 1000000);
                updateGraph();
                diff = 0;

            }
        }

    }

    private LoggerController controller = null;
    private UpdateTimer updateTimer;
    private NumberAxis xA;
    private NumberAxis yA;

    public GraphView(String chartName, String dataName, LoggerController controller) {
        this.updateTimer = new UpdateTimer();
        this.chartName = chartName;
        this.plotData = new ConcurrentHashMap<String, PlotInterval>();
        this.xA = new NumberAxis();
        this.yA = new NumberAxis();
        xA.setAutoRanging(true);
//        this.graph = new AreaChart<Number, Number>(xA, yA);
        this.graph = new LineChart<Number, Number>(xA, yA);
//        this.graph.setAnimated(true);
        this.graph.setTitle(chartName);
        this.dataName = dataName;
        this.controller = controller;

    }

    public String getDataName() {
        return dataName;
    }

    public void setDataName(String dataName) {
        this.dataName = dataName;
    }
    ArrayList<DestinationMetaData> destinations = null;
    StatType myType = StatType.UNKNOWN;

    public void updateDestinationList(ArrayList<DestinationMetaData> gdest) {
        if (gdest.isEmpty()) {
            return;
        }
        destinations = gdest;
        StatType a = StatType.UNKNOWN;
        if (gdest.get(0).getPathType() == PathType.REQUESTQUEUE) {
            a = StatType.REQUESTQUEUE;
        } else if (gdest.get(0).getPathType() == PathType.SERVICE) {
            a = StatType.SERVICEHANDLER;
        } else {
            return;
        }
        this.myType = a;
        controller.requestUsageData(dataName, this, gdest.get(0), a);
    }

    public void updateGraph() {
        if (destinations == null || myType == StatType.UNKNOWN || destinations.isEmpty()) {
            return;
        }
        controller.requestUsageData(dataName, this, destinations.get(0), myType);
    }

    public void startAutoUpdate() {
        this.updateTimer.start();
    }

    public void stopAutoUpdate() {
        this.updateTimer.stop();
    }

    public void displayData() {
        TimeInterval[] data = dataQueue.poll();
        if (data == null) {
            return;
        }
        PlotInterval plot = this.plotData.get(this.dataName);
        if (plot == null) {
            System.out.println("New plot");
            plot = new PlotInterval(this.dataName);
            plot.addTimeInterval(data);
            graph.getData().addAll(plot.getIn(), plot.getOut());
            this.plotData.putIfAbsent(this.dataName, plot);
            return;
        }

        plot.addTimeInterval(data);
    }

    final Object lock = new Object();

    private ConcurrentLinkedQueue<TimeInterval[]> dataQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void pushData(String name, TimeInterval[] data) {
        if (name == null) {
            return;
        }
        this.dataName = name;
        this.dataQueue.add(data);
        //
    }

    /**
     * Get the value of graph
     *
     * @return the value of graph
     */
    public LineChart<Number, Number> getGraph() {
        return graph;
    }

}
