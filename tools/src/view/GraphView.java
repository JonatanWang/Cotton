package view;

import Controller.LoggerController;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsRecorder.SampleRange;
import cotton.systemsupport.TimeInterval;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    private LineChart<Number, Number> graphIn;
    private LineChart<Number, Number> graphOut;
    private ConcurrentHashMap<DestinationMetaData, PlotInterval> plotData;
    private String chartName;
    private String dataName;
    private long gTickTime = 0;

    private class PlotInterval {

        XYChart.Series in;
        XYChart.Series out;
        long startTime = 0;
        int maxPoint;
        String myName;
        SampleRange range = null;

        public PlotInterval(String name, long currentTime) {
            this.myName = name;
            this.in = new XYChart.Series();
            this.out = new XYChart.Series();
            this.in.setName(name + currentTime);
            this.out.setName(name + currentTime);
            this.startTime = currentTime;
            this.maxPoint = gxMax;
        }

        public SampleRange getRange() {
            return range;
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

        public void addTimeInterval(TimeInterval[] data, SampleRange range) {
            this.range = range;
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
                //xA.setLowerBound(0);
                System.out.println("In; rsize: " + this.out.getData().size());
            }

            if (this.out.getData().size() > this.maxPoint) {
                this.out.getData().remove(0, this.out.getData().size() - this.maxPoint - 1);
                System.out.println("out; rsize: " + this.out.getData().size());
            }
//            if(startTime > 20) {
//                startTime = 0;
//            }

            ///    xA.setLowerBound(0);
            gTickTime = (gTickTime < startTime) ? startTime : gTickTime;
            System.out.println("start time: " + startTime + " size: in: " + this.in.getData().size() + " out:" + this.out.getData().size());
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
    private int gxMax = 25;

    public GraphView(String chartName, String dataName, LoggerController controller) {
        this.updateTimer = new UpdateTimer();
        this.chartName = chartName;
        this.plotData = new ConcurrentHashMap<>();
        this.xA = new NumberAxis(0, 20, 20 / 10);
        this.xA.setForceZeroInRange(false);
        this.xA.setAutoRanging(false);
        this.yA = new NumberAxis();
        yA.setAutoRanging(true);
//        this.graph = new AreaChart<Number, Number>(xA, yA);
        this.graphIn = new LineChart<Number, Number>(xA, yA);
        this.graphOut = new LineChart<Number, Number>(xA, yA);
        this.destinations = new HashSet<>();
//        this.graph.setAnimated(true);
        this.graphIn.setTitle(chartName + " in");
        this.graphOut.setTitle(chartName + " out");
        this.dataName = dataName;
        this.controller = controller;

    }

    public String getDataName() {
        return dataName;
    }

    public void setDataName(String dataName) {
        this.dataName = dataName;
    }
    HashSet<DestinationMetaData> destinations = null;
    StatType myType = StatType.UNKNOWN;

    public void updateDestinationList(ArrayList<DestinationMetaData> gdest) {
        if (gdest.isEmpty()) {
            return;
        }
        destinations.addAll(gdest);//= gdest;
        //controller.requestUsageData(dataName, this, gdest.get(0));
    }

    public void updateGraph() {
        if (destinations == null) {
            return;
        }
        for (DestinationMetaData d : destinations) {
            PlotInterval get = this.plotData.get(d);
            SampleRange range = null;
            if (get != null) {
                range = get.getRange();
            }
            controller.requestUsageData(dataName, this, d, range);
        }
    }

    public void startAutoUpdate() {
        this.updateTimer.start();
    }

    public void stopAutoUpdate() {
        this.updateTimer.stop();
    }

    private class QData {

        DestinationMetaData destination;
        SampleRange range;
        TimeInterval[] data;

        public QData(DestinationMetaData destination, SampleRange range, TimeInterval[] data) {
            this.destination = destination;
            this.range = range;
            this.data = data;
        }

        public DestinationMetaData getDestination() {
            return destination;
        }

        public SampleRange getRange() {
            return range;
        }

        public TimeInterval[] getData() {
            return data;
        }

    }

    public void displayData() {
        QData qd = null;
        while ((qd = dataQueue.poll()) != null) {
            if (qd == null) {
                break;
            }
            TimeInterval[] data = qd.getData();
            if (data == null) {
                continue;
            }
            PlotInterval plot = this.plotData.get(qd.destination);
            if (plot == null) {
                System.out.println("New plot");
                plot = new PlotInterval(this.dataName, this.gTickTime);
                plot.addTimeInterval(data, qd.getRange());
                graphIn.getData().addAll(plot.getIn());
                graphOut.getData().addAll(plot.getOut());
                this.plotData.putIfAbsent(qd.destination, plot);
                continue;
            }
            plot.addTimeInterval(data, qd.getRange());
        }
        xA.setUpperBound(this.gTickTime - 1);
        xA.setLowerBound(this.gTickTime - this.gxMax);
    }

    final Object lock = new Object();

    private ConcurrentLinkedQueue<QData> dataQueue = new ConcurrentLinkedQueue<>();

    @Override
    public void pushData(String name, DestinationMetaData destination, TimeInterval[] data, SampleRange range) {
        if (name == null) {
            return;
        }
        this.dataQueue.add(new QData(destination, range, data));
    }

    /**
     * Get the value of graph
     *
     * @return the value of graph
     */
    public LineChart<Number, Number> getInGraph() {
        return this.graphIn;
    }
    /**
     * Get the value of graph
     *
     * @return the value of graph
     */
    public LineChart<Number, Number> getOutGraph() {
        return this.graphOut;
    }

}
