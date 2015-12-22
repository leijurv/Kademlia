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
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
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
        //Line Chart
        CategoryAxis bytesChartxAxis = new CategoryAxis();
        bytesChartxAxis.setLabel("seconds");
        NumberAxis bytesChartyAxis = new NumberAxis();
        bytesChartyAxis.setLabel("bytes");
        LineChart<String, Number> bytesChart = new LineChart<>(bytesChartxAxis, bytesChartyAxis);
        bytesChart.setAnimated(false);
        bytesChart.setCreateSymbols(false);
        bytesChart.setTitle("Bytes Stored");

        XYChart.Series bytesChartRAMSeries = new XYChart.Series();
        bytesChartRAMSeries.setName("RAM");
        XYChart.Series bytesChartDiskSeries = new XYChart.Series();
        bytesChartDiskSeries.setName("Disk");
        XYChart.Series bytesChartTotalSeries = new XYChart.Series();
        bytesChartTotalSeries.setName("Total");

        bytesChart.getData().addAll(bytesChartRAMSeries, bytesChartDiskSeries, bytesChartTotalSeries);

        grid.add(bytesChart, 0, 1, 4, 4);

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        Runnable getBytesForLabel = () -> {
            Platform.runLater(() -> {
                int timeCounter = bytesChartTotalSeries.getData().size();
                bytesChartRAMSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter * 5), kad.storedData.bytesStoredInRAM()));
                bytesChartDiskSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter * 5), kad.storedData.bytesStoredOnDisk()));
                bytesChartTotalSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter * 5), kad.storedData.bytesStoredInTotal()));
                if (timeCounter == 12) {
                    bytesChartRAMSeries.getData().remove(0, bytesChartRAMSeries.getData().size());
                    bytesChartDiskSeries.getData().remove(0, bytesChartDiskSeries.getData().size());
                    bytesChartTotalSeries.getData().remove(0, bytesChartTotalSeries.getData().size());
                }
                bytesStoredInRAMLabel.setText("Data Stored in Ram: " + humanReadableByteCount(kad.storedData.bytesStoredInRAM(), true));
                bytesStoredOnDiskLabel.setText("Data Stored on Disk: " + humanReadableByteCount(kad.storedData.bytesStoredOnDisk(), true));
                bytesStoredTotalLabel.setText("Data Stored in Total: " + humanReadableByteCount(kad.storedData.bytesStoredInTotal(), true));
            });
        };
        executor.scheduleAtFixedRate(getBytesForLabel, 0, 1, TimeUnit.SECONDS);

        //Tab
        this.setContent(grid);
    }

    public static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

}
