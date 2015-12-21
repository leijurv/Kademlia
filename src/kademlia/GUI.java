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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class GUI extends Application {
    private static final ObservableList<KeyValueData> keyValueDataList = FXCollections.observableArrayList();
    private static Kademlia kad;
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
        GridPane dataGrid = new GridPane();
        dataGrid.setAlignment(Pos.TOP_CENTER);
        for (int i = 0; i < 10; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(10);
            dataGrid.getColumnConstraints().add(col);
        }
        for (int i = 0; i < 10; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(10);
            dataGrid.getRowConstraints().add(row);
        }
        //dataGrid.setHgap(10);
        //dataGrid.setVgap(10);
        //dataGrid.setPadding(new Insets(25, 25, 25, 25));
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
        dataGrid.add(table, 0, 0, 10, 5);
        dataGrid.setGridLinesVisible(true);
        TextField getValueTextField = new TextField();
        getValueTextField.setPromptText("Key");
        dataGrid.add(getValueTextField, 0, 5, 3, 1);
        Button getBtn = new Button();
        getBtn.setText("Get Value");
        getBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (getValueTextField.getText().length() == 0) {
                    return;
                }
                System.out.println("your mom " + getValueTextField.getText());
                kad.get(getValueTextField.getText());
            }
        });
        dataGrid.add(getBtn, 3, 5);
        tab.setContent(dataGrid);
        tabPane.getTabs().add(tab);
        Scene scene = new Scene(tabPane, 1000, 750);
        primaryStage.setTitle("Kademlia GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    public static void main(String[] args, Kademlia kademliaRef) {
        kad = kademliaRef;
        launch(args);
    }
    public static void incomingKeyValueData(long rawKey, byte[] rawValue) {
        keyValueDataList.add(new KeyValueData(rawKey, rawValue));
    }
}
