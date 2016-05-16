package experimentallogger;

import Controller.LoggerController;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import model.CloudStat;
import view.MainView;

/**
 *
 * @author Magnus
 * @author Mats
 */
public class ExperimentalLogger extends Application {
    private LoggerController controll = null;
    private CloudStat cloudStat = null;
    private MainView view = null;
    @Override
    public void start(Stage primaryStage) {
        
        
        try {
            cloudStat = new CloudStat();
        } catch (UnknownHostException ex) {
            Logger.getLogger(ExperimentalLogger.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        view = new MainView();
        controll = new LoggerController(view,cloudStat,primaryStage);
        view.setup();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                controll.disconnectCloud();
            }
        });
        controll.showScene(primaryStage);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
