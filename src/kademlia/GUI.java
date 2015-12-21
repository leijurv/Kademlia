/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Border;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class GUI extends Application {
    private static final ObservableList<KeyValueData> keyValueDataList = FXCollections.observableArrayList();
    private static Kademlia kad;
    private static ProgressBar fileProgressBar;
    @Override
    public void start(Stage primaryStage) {
        // Tab
        TabPane tabPane = new TabPane();
        DataGUITab dataTab = new DataGUITab(primaryStage, kad);
        
        //Tab
        tabPane.getTabs().add(dataTab);
        //Scene
        Scene scene = new Scene(tabPane, 1000, 750);
        primaryStage.setTitle("Kademlia GUI");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();
    }
    public static void main(String[] args, Kademlia kademliaRef) {
        kad = kademliaRef;
        launch(args);
    }
}
