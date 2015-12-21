/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.File;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class DataGUITab extends Tab {
    private static final ObservableList<KeyValueData> keyValueDataList = FXCollections.observableArrayList();
    private static Kademlia kad;
    private static ProgressBar fileProgressBar;
    DataGUITab(Stage primaryStage, Kademlia kademliaRef) {
        super();
        
        kad = kademliaRef;
        this.setText("Data");
        this.setClosable(false);
        
        GridPane dataGrid = new GridPane();
        dataGrid.setAlignment(Pos.TOP_CENTER);
        dataGrid.setHgap(10);
        dataGrid.setVgap(10);
        //dataGrid.setGridLinesVisible(true);
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
        //Table View
        TableView<KeyValueData> table = new TableView<>();
        TableColumn keyCol = new TableColumn("Key");
        keyCol.setCellValueFactory(new PropertyValueFactory<>("key"));
        keyCol.setPrefWidth(100);
        keyCol.setEditable(false);
        keyCol.setSortable(false);
        TableColumn valCol = new TableColumn("Value");
        valCol.setCellValueFactory(new PropertyValueFactory<>("value"));
        valCol.setPrefWidth(500);
        valCol.setEditable(true);
        valCol.setSortable(false);
        table.getColumns().addAll(keyCol, valCol);
        table.setItems(keyValueDataList);
        dataGrid.add(table, 0, 0, 10, 5);
        
        /* GET */
        //TextField
        TextField getValueTextField = new TextField();
        getValueTextField.setPromptText("Key");
        dataGrid.add(getValueTextField, 0, 5, 3, 1);
        //Button
        Button getBtn = new Button();
        getBtn.setText("Get Value");
        getBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (getValueTextField.getText().length() == 0) {
                    return;
                }
                kad.get(getValueTextField.getText());
                getValueTextField.clear();
            }
        });
        dataGrid.add(getBtn, 3, 5);
        /* PUT */
        //TextField
        TextField putKeyTextField = new TextField();
        putKeyTextField.setPromptText("Key");
        dataGrid.add(putKeyTextField, 5, 5, 1, 1);
        //TextField
        TextField putValueTextField = new TextField();
        putValueTextField.setPromptText("Value");
        dataGrid.add(putValueTextField, 6, 5, 2, 1);
        //Button
        Button putBtn = new Button();
        putBtn.setText("Put Key/Value");
        putBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (putKeyTextField.getText().length() == 0 || putValueTextField.getText().length() == 0) {
                    return;
                }
                kad.put(putKeyTextField.getText(), putValueTextField.getText().getBytes());
                incomingKeyValueData(Lookup.hash(putKeyTextField.getText().getBytes()), putValueTextField.getText().getBytes());
                putKeyTextField.clear();
                putValueTextField.clear();
            }
        });
        dataGrid.add(putBtn, 8, 5, 2, 1);
        /* PUT FILE */
        //TextField
        TextField putFileKeyTextField = new TextField();
        putFileKeyTextField.setPromptText("Key");
        dataGrid.add(putFileKeyTextField, 0, 7, 3, 1);
        //Button
        Button putFileBtn = new Button();
        putFileBtn.setText("Put File");
        putFileBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (putFileKeyTextField.getText().length() == 0) {
                    return;
                }
                //kad.put(putKeyTextField.getText(), putValueTextField.getText().getBytes());
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showOpenDialog(primaryStage);
                if (file != null) {
                    try {
                        kad.putfile(file, putFileKeyTextField.getText());
                    } catch (IOException | InterruptedException ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, ex.getLocalizedMessage());
                        alert.show();
                    }
                }
            }
        });
        dataGrid.add(putFileBtn, 3, 7);
        /* GET FILE */
        //TextField
        TextField getFileKeyTextField = new TextField();
        getFileKeyTextField.setPromptText("Key");
        dataGrid.add(getFileKeyTextField, 5, 7, 3, 1);
        //Button
        Button getFileBtn = new Button();
        getFileBtn.setText("Get File");
        getFileBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (getFileKeyTextField.getText().length() == 0) {
                    return;
                }
                //kad.put(putKeyTextField.getText(), putValueTextField.getText().getBytes());
                FileChooser fileChooser = new FileChooser();
                File file = fileChooser.showSaveDialog(primaryStage);
                if (file != null) {
                    try {
                        kad.getfile(getFileKeyTextField.getText(), file);
                    } catch (IOException ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, ex.getLocalizedMessage());
                        alert.show();
                    }
                }
            }
        });
        dataGrid.add(getFileBtn, 8, 7);
        /* Progress Bar */
        fileProgressBar = new ProgressBar();
        fileProgressBar.setProgress(0);
        fileProgressBar.setDisable(true);
        fileProgressBar.setPrefWidth(980);
        dataGrid.add(fileProgressBar, 1, 9, 8, 1);
        
        //Tab
        this.setContent(dataGrid);
    }
    public static void incomingKeyValueData(long rawKey, byte[] rawValue) {
        keyValueDataList.add(new KeyValueData(rawKey, rawValue));
    }
    public static void alertForError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.show();
    }
    public static void updateProgressBar(float progress) {
        if (fileProgressBar.isDisabled()) {
            fileProgressBar.setDisable(false);
        }
        fileProgressBar.setProgress(progress);
        if (progress == 1) {
            fileProgressBar.setDisable(true);
            fileProgressBar.setProgress(0);
        }
    }
}
