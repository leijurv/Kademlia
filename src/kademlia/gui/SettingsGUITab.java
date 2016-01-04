/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.gui;

import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;
import kademlia.Kademlia;

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
        int units = (int) Math.floor(Math.log(kad.settings.maxRAMSizeBytes) / Math.log(1024));
        maxRamNumberSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1000, (int) (kad.settings.maxRAMSizeBytes / Math.pow(1024, units))));
        ChoiceBox maxRamUnitCB = new ChoiceBox();
        maxRamUnitCB.getItems().addAll("Bytes", "KB", "MB", "GB");
        maxRamUnitCB.getSelectionModel().select(units);
        grid.add(maxRamLabel, 0, 0);
        grid.add(maxRamNumberSpinner, 1, 0);
        grid.add(maxRamUnitCB, 2, 0);
        /* Garbage Collection Interval */
        Label gcIntervalLabel = new Label("Garbage Collection Interval:");
        gcIntervalLabel.setAlignment(Pos.CENTER);
        Spinner gcIntervalSpinner = new Spinner();
        gcIntervalSpinner.setEditable(false);
        gcIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 120, kad.settings.garbageCollectionIntervalSec));
        Label gcIntervalSecondsLabel = new Label("seconds");
        grid.add(gcIntervalLabel, 4, 0, 2, 1);
        grid.add(gcIntervalSpinner, 6, 0);
        grid.add(gcIntervalSecondsLabel, 7, 0);
        /* Ping Timeout */
        Label pingTimeoutLabel = new Label("Ping Timeout:");
        pingTimeoutLabel.setAlignment(Pos.CENTER);
        Spinner pingTimeoutSpinner = new Spinner();
        pingTimeoutSpinner.setEditable(false);
        pingTimeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 30, kad.settings.pingTimeoutSec));
        Label pingTimeoutSecondsLabel = new Label("seconds");
        grid.add(pingTimeoutLabel, 0, 1);
        grid.add(pingTimeoutSpinner, 1, 1);
        grid.add(pingTimeoutSecondsLabel, 2, 1);
        /* Ping Interval */
        Label pingIntervalLabel = new Label("Ping Interval:");
        pingIntervalLabel.setAlignment(Pos.CENTER);
        Spinner pingIntervalSpinner = new Spinner();
        pingIntervalSpinner.setEditable(false);
        pingIntervalSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 300, kad.settings.pingIntervalSec));
        Label pingIntervalSecondsLabel = new Label("seconds");
        grid.add(pingIntervalLabel, 4, 1);
        grid.add(pingIntervalSpinner, 5, 1);
        grid.add(pingIntervalSecondsLabel, 6, 1);
        /* Update Data Timeout */
        Label updateDataTimeoutLabel = new Label("Update Data Timeout:");
        updateDataTimeoutLabel.setAlignment(Pos.CENTER);
        Spinner updateDataTimeoutSpinner = new Spinner();
        updateDataTimeoutSpinner.setEditable(false);
        updateDataTimeoutSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 300, kad.settings.pingTimeoutSec));
        Label updateDataTimeoutSecondsLabel = new Label("seconds");
        grid.add(updateDataTimeoutLabel, 0, 2);
        grid.add(updateDataTimeoutSpinner, 1, 2);
        grid.add(updateDataTimeoutSecondsLabel, 2, 2);

        /* Save Settings */
        Button saveBtn = new Button();
        saveBtn.setText("Save Settings");
        saveBtn.setDefaultButton(true);
        saveBtn.setOnAction((ActionEvent event) -> {
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
            kad.settings.pingTimeoutSec = (int) pingTimeoutSpinner.getValue();
            kad.settings.pingIntervalSec = (int) pingIntervalSpinner.getValue();
            kad.settings.updateIntervalSec = (int) updateDataTimeoutSpinner.getValue();
            kad.settings.onChange();
        });
        grid.add(saveBtn, 1, 9, 2, 1);

        //Tab
        this.setContent(grid);
    }
}
