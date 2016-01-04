/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.gui;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import kademlia.Kademlia;

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
        ConnectionGUITab connectionTab = new ConnectionGUITab(primaryStage, kad);
        NodeInfoGUITab nodeInfoTab = new NodeInfoGUITab(primaryStage, kad);
        SettingsGUITab settingsTab = new SettingsGUITab(primaryStage, kad);
        DebugGUITab debugTab = new DebugGUITab(primaryStage);
        
        //Tab
        tabPane.getTabs().addAll(dataTab, connectionTab, nodeInfoTab, settingsTab, debugTab);
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
