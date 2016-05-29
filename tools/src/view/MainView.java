package view;

import Controller.LoggerController;
import cotton.network.DestinationMetaData;
import cotton.network.PathType;
import cotton.systemsupport.StatType;
import cotton.systemsupport.StatisticsData;
import cotton.systemsupport.TimeInterval;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 *
 * @author Magnus
 * @author Mats
 */
public class MainView {

    private Scene scene = null;

    public MainView() {
    }

    public static class NodeInfo {

        private final StringProperty sName;
        private final StringProperty type;// = new SimpleStringProperty();
        private final StringProperty ip;// = new SimpleStringProperty();
        private final StringProperty port;// = new SimpleStringProperty();

        public NodeInfo(String sName, String type, String ip, String port) {
            this.sName = new SimpleStringProperty(sName);
            this.type = new SimpleStringProperty(type);
            this.ip = new SimpleStringProperty(ip);
            this.port = new SimpleStringProperty(port);
        }

        public String getPort() {
            return port.get();
        }

        public void setPort(String value) {
            port.set(value);
        }

        public StringProperty portProperty() {
            return port;
        }

        public String getIp() {
            return ip.get();
        }

        public void setIp(String value) {
            ip.set(value);
        }

        public StringProperty ipProperty() {
            return ip;
        }

        public String getType() {
            return type.get();
        }

        public void setType(String value) {
            type.set(value);
        }

        public StringProperty typeProperty() {
            return type;
        }

        public String getsName() {
            return sName.get();
        }

        public void setsName(String value) {
            sName.set(value);
        }

        public StringProperty sNameProperty() {
            return sName;
        }

    }

    private LoggerController controller;

    public void setDelegate(LoggerController controller) {
        this.controller = controller;
    }
    private ObservableList<NodeInfo> discoveryTableObs = FXCollections.observableArrayList();
    private ObservableList<NodeInfo> requestQueueTableObs = FXCollections.observableArrayList();
    private ObservableList<NodeInfo> serviceTableObs = FXCollections.observableArrayList();
    //private TableView table = new TableView();

    private VBox tableBox(String boxName, Button updateBtn, ObservableList<NodeInfo> obsList) {
        Label label = new Label(boxName);
        TableView table = new TableView();
        table.setEditable(true);

        TableColumn serviceName = new TableColumn("Service name");
        serviceName.setMinWidth(100);
        serviceName.setCellValueFactory(new PropertyValueFactory<NodeInfo, String>("sName"));
        TableColumn type = new TableColumn("Service type");
        type.setMinWidth(100);
        type.setCellValueFactory(new PropertyValueFactory<NodeInfo, String>("type"));
        TableColumn ip = new TableColumn("ip");
        ip.setMinWidth(100);
        ip.setCellValueFactory(new PropertyValueFactory<NodeInfo, String>("ip"));
        TableColumn port = new TableColumn("port");
        port.setMinWidth(100);
        port.setCellValueFactory(new PropertyValueFactory<NodeInfo, String>("port"));
        table.setItems(obsList);
        table.getColumns().addAll(serviceName, type, ip, port);

        final VBox box = new VBox();
        box.setSpacing(5);
        box.setPadding(new Insets(10, 0, 0, 10));
        box.getChildren().addAll(label, table, updateBtn);
        return box;
    }

    //private DestinationMetaData cloudDest = null;
    private HBox ipConnectArea() {
        final TextField ipField = new TextField();
        final TextField portField = new TextField();
        ipField.setPromptText("Enter ip");
        try {
            ipField.setText(Inet4Address.getLocalHost().getHostAddress());
        } catch (UnknownHostException ex) {
            Logger.getLogger(MainView.class.getName()).log(Level.SEVERE, null, ex);
        }
        portField.setPromptText("Enter port");

        Button btn = new Button();
        btn.setText("Connect cloud");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String ip = ipField.getText();
                String tmp = portField.getText();
                int port = Integer.parseInt(tmp);
                controller.connectCloud(ip, port);
                //controller.
                System.out.println("connecting");
            }
        });

        Button shut = new Button();
        shut.setText("disconnect cloud");
        shut.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                controller.disconnectCloud();
                System.out.println("disconnecting");
            }
        });
        Button updateAll = new Button();
        updateAll.setText("Update All");
        updateAll.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                updateAll();
                //graphRequestQueue.displayData();
                //graphService.displayData();
            }
        });

        final TextField graphField = new TextField();
        graphField.setPromptText("enter service name");
        Button updateGraph = new Button();
        updateGraph.setText("Update Graph");
        updateGraph.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                graphRequestQueue.setDataName(graphField.getText());
                graphService.setDataName(graphField.getText());
                
                //graphRequestQueue.startAutoUpdate();
                //graphService.startAutoUpdate();
                
            }
        });
        final HBox box = new HBox();
        box.setSpacing(5);
        box.setPadding(new Insets(10, 0, 0, 10));
        box.getChildren().addAll(ipField, portField, shut, btn, updateAll, graphField, updateGraph);
        return box;
    }

    private GraphView graphRequestQueue;
    private GraphView graphService;
    
    private interface Change<T, U> {

        public T work(U u);
    }

    private interface Check<T> {

        public boolean check(T t);
    }

    private static <T, U> T[] mapFunc(U[] uArr, T[] res, Change<T, U> c) {
        //T[] res = (T[]) new Object[uArr.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = c.work(uArr[i]);
        }
        return res;
    }

    private static <T> ArrayList<T> reduceFunc(T[] tArr, Check<T> c) {
        ArrayList<T> res = new ArrayList();
        for (int i = 0; i < tArr.length; i++) {
            T t = tArr[i];
            if (c.check(t)) {
                res.add(t);
            }
        }
        return res;
    }

    

    private boolean checkDest(DestinationMetaData u, PathType type) {
        return u.getPathType() == type;
    }

    private NodeInfo fillNodeInfo(DestinationMetaData u, String name) {
        InetSocketAddress addr = (InetSocketAddress) u.getSocketAddress();
        String ip = addr.getAddress().getHostAddress();
        Integer port = new Integer(addr.getPort());
        return new NodeInfo(name, u.getPathType().toString(), ip, port.toString());
    }

    public void updateGraph(String name, DataPusherGraph graph, DestinationMetaData dest, StatType type) {
        this.controller.requestUsageData(name, graph, dest,null);
    }

    public void parseData(ObservableList<NodeInfo> oblist, ArrayList<StatisticsData<DestinationMetaData>> res) {
        //System.out.println("Arr" + res.toString());
        oblist.clear();
        for (StatisticsData<DestinationMetaData> entry : res) {
            String name = entry.getName();
            DestinationMetaData[] dest = entry.getData();
            NodeInfo[] nodeInfo = mapFunc(dest, new NodeInfo[dest.length], new Change<NodeInfo, DestinationMetaData>() {
                @Override
                public NodeInfo work(DestinationMetaData u) {
                    return fillNodeInfo(u, name);
                }
            });
            oblist.addAll(nodeInfo);
            if (name.equals(graphRequestQueue.getDataName())) {
                ArrayList<DestinationMetaData> gdest = reduceFunc(dest, new Check<DestinationMetaData>() {
                    @Override
                    public boolean check(DestinationMetaData t) {
                        return checkDest(t,PathType.REQUESTQUEUE);
                    }
                });
                graphRequestQueue.updateDestinationList(gdest);
            }
            if(name.equals(graphService.getDataName())){
                ArrayList<DestinationMetaData> gdest = reduceFunc(dest, new Check<DestinationMetaData>() {
                    @Override
                    public boolean check(DestinationMetaData t) {
                        return checkDest(t,PathType.SERVICE);
                    }
                });
                graphService.updateDestinationList(gdest);
            }
            //            }
        }
    }

    private NodeInfo discG = null;

    private void updateAll() {
        parseData(discoveryTableObs, controller.getNodesFor(StatType.DISCOVERY));
        parseData(requestQueueTableObs, controller.getNodesFor(StatType.REQUESTQUEUE));
        parseData(serviceTableObs, controller.getNodesFor(StatType.SERVICEHANDLER));
    }

    private TimeInterval[] generateRandomData(int size) {
        TimeInterval[] gtest = new TimeInterval[size];
        Random rnd = new Random();
        for (int i = 0; i < gtest.length; i++) {
            gtest[i] = new TimeInterval(1000);
            gtest[i].setInputCount(rnd.nextInt(25));
            gtest[i].setOutputCount(rnd.nextInt(25));
        }
        return gtest;
    }

    public void setup(/*DBAlbumVC dbController*/) {
        //BorderPane mainPane = new BorderPane();
        StackPane mainPane = new StackPane();
        Button discBtn = new Button();
        Button reqQBtn = new Button();
        Button servBtn = new Button();
        discBtn.setText("Update");
        reqQBtn.setText("Update");
        servBtn.setText("Update");
        discBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                parseData(discoveryTableObs, controller.getNodesFor(StatType.DISCOVERY));
                System.out.println("not implemented");
            }
        });
        reqQBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                parseData(requestQueueTableObs, controller.getNodesFor(StatType.REQUESTQUEUE));
                graphRequestQueue.updateGraph();
            }
        });
        servBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                parseData(serviceTableObs, controller.getNodesFor(StatType.SERVICEHANDLER));
                graphService.updateGraph();
            }
        });
        VBox tableBox1 = tableBox("Discovery", discBtn, this.discoveryTableObs);
        VBox tableBox2 = tableBox("Request Queue", reqQBtn, this.requestQueueTableObs);
        VBox tableBox3 = tableBox("Services", servBtn, this.serviceTableObs);
        graphRequestQueue = new GraphView("Request Queue","mathPow2",this.controller);
        graphService = new GraphView("Service Queue","mathPow2",this.controller);
        this.graphRequestQueue.startAutoUpdate();
        this.graphService.startAutoUpdate();
//        TimeInterval[] gtest = generateRandomData(10);
//        TimeInterval[] gtest1 = generateRandomData(10);
//        graphRequestQueue.pushData("testD", gtest);
//        graphService.pushData("testD1", gtest1);

        final HBox gqArea = new HBox();
        final HBox gsArea = new HBox();
        final HBox qio = new HBox();
        final HBox sio = new HBox();
        qio.setSpacing(5);
        sio.setSpacing(5);
        gqArea.setSpacing(5);
        gsArea.setSpacing(5);
        qio.setPadding(new Insets(10, 0, 0, 10));
        sio.setPadding(new Insets(10, 0, 0, 10));
        gqArea.setPadding(new Insets(10, 0, 0, 10));
        gsArea.setPadding(new Insets(10, 0, 0, 10));
        qio.getChildren().addAll(graphRequestQueue.getInGraph(),graphRequestQueue.getOutGraph());
        sio.getChildren().addAll(graphService.getInGraph(),graphService.getOutGraph());
        gqArea.getChildren().addAll(tableBox2, qio);
        gsArea.getChildren().addAll(tableBox3, sio);
        final VBox monitorArea = new VBox();
        monitorArea.setSpacing(5);
        monitorArea.setPadding(new Insets(10, 0, 0, 10));
        monitorArea.getChildren().addAll(tableBox1, gqArea, gsArea);
        HBox ipConnectArea = ipConnectArea();
        VBox mainArea = new VBox();
        mainArea.setSpacing(5);
        mainArea.setPadding(new Insets(10, 0, 0, 10));
        mainArea.getChildren().addAll(monitorArea, ipConnectArea);

        mainPane.getChildren().addAll(mainArea);

        this.scene = new Scene(mainPane, 800, 500);
        this.graphRequestQueue.startAutoUpdate();
        this.graphService.startAutoUpdate();
        final AnimationTimer t = new AnimationTimer(){
            @Override
            public void handle(long now) {
                System.out.println("Test: now " + now);
            }
        };
        //t.start();

    }
    
    public void shutdown() {
        this.graphRequestQueue.stopAutoUpdate();
        this.graphService.stopAutoUpdate();
    }

    public void showScene(Stage rootStage) {

        rootStage.setTitle("Control center overview");
        rootStage.setScene(scene);
        rootStage.sizeToScene();
        rootStage.show();
    }

}
