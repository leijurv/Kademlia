/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class GUI extends Application
{
    private final ObservableList<KeyValueData> keyValueDataList = FXCollections.observableArrayList();

    
   @Override
    public void start(Stage primaryStage) {
        /*Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                System.out.println("Hello World!");
            }
        });*/
        TabPane tabPane = new TabPane();
        Tab tab = new Tab();
        tab.setClosable(false);
        tab.setText("Data");
        TableView<KeyValueData> table = new TableView<>();
        TableColumn keyCol = new TableColumn("Key");
        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyCol.setEditable(false);
        keyCol.setSortable(false);
        TableColumn valCol = new TableColumn("Value");
        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valCol.setEditable(true);
        valCol.setSortable(false);
        table.getColumns().addAll(keyCol, valCol);
        table.setItems(keyValueDataList);
        tab.setContent(table);
        tabPane.getTabs().add(tab);

        Scene scene = new Scene(tabPane, 300, 250);

        primaryStage.setTitle("Kademlia GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
    public void incomingKeyValueData(long rawKey, byte[] rawValue) {
        keyValueDataList.add(new KeyValueData(rawKey, rawValue));
    }
    
}