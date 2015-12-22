/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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
import static kademlia.DataGUITab.incomingKeyValueData;

/**
 *
 * @author aidan
 */
public class NodeInfoGUITab extends Tab {
    private static Kademlia kad;
    NodeInfoGUITab(Stage primaryStage, Kademlia kademliaRef) {
        super();
        
        kad = kademliaRef;
        this.setText("Node Info");
        this.setClosable(false);

        
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
        /* Stored */
        //TextField
        Label bytesStoredInRAMLabel = new Label();
        Label bytesStoredOnDiskLabel = new Label();
        Label bytesStoredTotalLabel = new Label();
        grid.add(bytesStoredInRAMLabel, 0, 0, 2, 1);
        grid.add(bytesStoredOnDiskLabel, 2, 0, 2, 1);
        grid.add(bytesStoredTotalLabel, 4, 0, 2, 1);
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable getBytesForLabel = () -> {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    bytesStoredInRAMLabel.setText("Bytes Stored in Ram: " + kad.storedData.bytesStoredInRAM());
                    bytesStoredOnDiskLabel.setText("Bytes Stored on Disk: " + kad.storedData.bytesStoredOnDisk());
                    bytesStoredTotalLabel.setText("Bytes Stored in Total: " + kad.storedData.bytesStoredInTotal());
                }
            });
        };
        executor.scheduleAtFixedRate(getBytesForLabel, 0, 5, TimeUnit.SECONDS);
        
        //Tab
        this.setContent(grid);
    }
    
}
