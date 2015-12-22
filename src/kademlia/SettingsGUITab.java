/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class SettingsGUITab extends Tab {
    private static Kademlia kad;
    SettingsGUITab(Stage primaryStage, Kademlia kad) {
        super();
        
        this.setText("Settings");
        this.setClosable(false);

        this.kad = kad;
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        //dataGrid.setGridLinesVisible(true);
        for (int i = 0; i < 10; i++) {
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(10);
            grid.getColumnConstraints().add(col);
        }
        for (int i = 0; i < 10; i++) {
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(10);
            grid.getRowConstraints().add(row);
        }
        /* Max RAM Size */
        Label maxRamLabel = new Label("Max Ram:");
        maxRamLabel.setAlignment(Pos.CENTER);
        Spinner maxRamNumberSpinner = new Spinner();
        maxRamNumberSpinner.setEditable(false);
        maxRamNumberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1000));
        ChoiceBox maxRamUnitCB = new ChoiceBox();
        maxRamUnitCB.getItems().addAll("Bytes", "KB", "MB", "GB");
        maxRamUnitCB.getSelectionModel().selectFirst();
        grid.add(maxRamLabel, 0, 0);
        grid.add(maxRamNumberSpinner, 1, 0);
        grid.add(maxRamUnitCB, 2, 0);
        /* Garbage Collection Interval */
        Label gcIntervalLabel = new Label("Garbage Collection Interval:");
        gcIntervalLabel.setAlignment(Pos.CENTER);
        Spinner gcIntervalSpinner = new Spinner();
        gcIntervalSpinner.setEditable(false);
        gcIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120));
        Label gcIntervalSecondsLabel = new Label("seconds");
        grid.add(gcIntervalLabel, 4, 0, 2, 1);
        grid.add(gcIntervalSpinner, 6, 0);
        grid.add(gcIntervalSecondsLabel, 7, 0);
        
        /* Save Settings */
        Button saveBtn = new Button();
        saveBtn.setText("Save Settings");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                kad.settings.garbageCollectionIntervalSec = (int) gcIntervalSpinner.getValue();
                int maxRamMultiple = 1;
                switch (maxRamUnitCB.getValue().toString()) {
                    case "Bytes":
                        break;
                    case "KB":
                        maxRamMultiple = 1024;
                        break;
                    case "MB":
                        maxRamMultiple = 1024 * 1024;
                        break;
                    case "GB":
                        maxRamMultiple = 1024 * 1024 * 1024;
                        break;
                }
                kad.settings.maxRAMSizeBytes = (int) maxRamNumberSpinner.getValue() * maxRamMultiple;
                kad.settings.onChange();
            }
        });
        grid.add(saveBtn, 1, 9, 2, 1);
        
        //Tab
        this.setContent(grid);
    }
}
