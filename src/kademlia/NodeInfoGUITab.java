/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

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
            Platform.runLater(() -> {
                bytesStoredInRAMLabel.setText("Data Stored in Ram: " + humanReadableByteCount(kad.storedData.bytesStoredInRAM(), true));
                bytesStoredOnDiskLabel.setText("Data Stored on Disk: " + humanReadableByteCount(kad.storedData.bytesStoredOnDisk(), true));
                bytesStoredTotalLabel.setText("Data Stored in Total: " + humanReadableByteCount(kad.storedData.bytesStoredInTotal(), true));
            });
        };
        executor.scheduleAtFixedRate(getBytesForLabel, 0, 5, TimeUnit.SECONDS);
        
        //Tab
        this.setContent(grid);
    }
    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }
    
}
