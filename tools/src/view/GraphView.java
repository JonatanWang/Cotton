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
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

/**
 *
 * @author Magnus
 * @author Mats
 */
public class GraphView implements DataPusherGraph<TimeInterval> {

    private AreaChart<Number, Number> graph;
    private ConcurrentHashMap<String, PlotInterval> plotData;
    private String chartName;
    private String dataName;

    private class PlotInterval {

        XYChart.Series in;
        XYChart.Series out;
        long startTime = 0;

        public PlotInterval(String name) {
            this.in = new XYChart.Series();
            this.out = new XYChart.Series();
            this.in.setName(name + " input");
            this.out.setName(name + " output");
        }

        public XYChart.Series getIn() {
            return in;
        }

        public XYChart.Series getOut() {
            return out;
        }

        public void addTimeInterval(TimeInterval[] data) {
            startTime = 0;
            
            this.in.getData().clear();
            this.out.getData().clear();
            for (int i = 0; i < data.length; i++) {
                TimeInterval d = data[i];
                this.in.getData().add(new XYChart.Data(startTime, d.calculateInputIntensity()));
                this.out.getData().add(new XYChart.Data(startTime, d.calculateOutputIntensity()));
                startTime += d.getDeltaTime();
                System.out.println("");
            }
        }

    }
    private LoggerController controller = null;

    public GraphView(String chartName, String dataName, LoggerController controller) {
        this.chartName = chartName;
        this.plotData = new ConcurrentHashMap<String, PlotInterval>();
        final NumberAxis xA = new NumberAxis();
        final NumberAxis yA = new NumberAxis();
        this.graph = new AreaChart<Number, Number>(xA, yA);
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

    public void displayData() {
        TimeInterval[] data = dataQueue.poll();
        if(data == null) {
            return;
        }
        PlotInterval plot = this.plotData.get(this.dataName);
        if (plot == null) {
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
        dataQueue.add(data);
        //
    }

    /**
     * Get the value of graph
     *
     * @return the value of graph
     */
    public AreaChart<Number, Number> getGraph() {
        return graph;
    }

}
