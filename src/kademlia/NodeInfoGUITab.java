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
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
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
        Runnable getBytesForLabel = new Runnable() {
            int timeCounter = 0;
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        timeCounter++;
                        bytesChartRAMSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter), kad.storedData.bytesStoredInRAM()));
                        bytesChartDiskSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter), kad.storedData.bytesStoredOnDisk()));
                        bytesChartTotalSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter), kad.storedData.bytesStoredInTotal()));
                        if (timeCounter > 20) {
                            bytesChartRAMSeries.getData().remove(0, 1);
                            bytesChartDiskSeries.getData().remove(0, 1);
                            bytesChartTotalSeries.getData().remove(0, 1);
                        }
                        bytesStoredInRAMLabel.setText("Data Stored in Ram: " + humanReadableByteCount(kad.storedData.bytesStoredInRAM(), true));
                        bytesStoredOnDiskLabel.setText("Data Stored on Disk: " + humanReadableByteCount(kad.storedData.bytesStoredOnDisk(), true));
                        bytesStoredTotalLabel.setText("Data Stored in Total: " + humanReadableByteCount(kad.storedData.bytesStoredInTotal(), true));
                    }
                });
            }
        };
        executor.scheduleAtFixedRate(getBytesForLabel, 0, 1, TimeUnit.SECONDS);
        CategoryAxis linesChartxAxis = new CategoryAxis();
        linesChartxAxis.setLabel("seconds");
        NumberAxis linesChartyAxis = new NumberAxis();
        linesChartyAxis.setLabel("Number of items");
        LineChart<String, Number> linesChart = new LineChart<>(linesChartxAxis, linesChartyAxis);
        linesChart.setAnimated(false);
        linesChart.setCreateSymbols(false);
        linesChart.setTitle("Items Stored");
        XYChart.Series linesChartRAMSeries = new XYChart.Series();
        linesChartRAMSeries.setName("RAM");
        XYChart.Series linesChartDiskSeries = new XYChart.Series();
        linesChartDiskSeries.setName("Disk");
        XYChart.Series linesChartTotalSeries = new XYChart.Series();
        linesChartTotalSeries.setName("Total");
        linesChart.getData().addAll(linesChartRAMSeries, linesChartDiskSeries, linesChartTotalSeries);
        grid.add(linesChart, 6, 1, 4, 4);
        Runnable getItemsForLabel = new Runnable() {
            int timeCounter = 0;
            @Override
            public void run() {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        timeCounter++;
                        linesChartRAMSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter), kad.storedData.itemsStoredInRAM()));
                        linesChartDiskSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter), kad.storedData.itemsStoredOnDisk()));
                        linesChartTotalSeries.getData().add(new XYChart.Data(Integer.toString(timeCounter), kad.storedData.itemsStoredInTotal()));
                        if (timeCounter > 20) {
                            linesChartRAMSeries.getData().remove(0, 1);
                            linesChartDiskSeries.getData().remove(0, 1);
                            linesChartTotalSeries.getData().remove(0, 1);
                        }
                        //bytesStoredInRAMLabel.setText("Items Stored in Ram: " + humanReadableByteCount(kad.storedData.itemsStoredInRAM(), true));
                        //bytesStoredOnDiskLabel.setText("Items Stored on Disk: " + humanReadableByteCount(kad.storedData.itemsStoredOnDisk(), true));
                        //bytesStoredTotalLabel.setText("Items Stored in Total: " + humanReadableByteCount(kad.storedData.itemsStoredInTotal(), true));
                    }
                });
            }
        };
        executor.scheduleAtFixedRate(getItemsForLabel, 0, 1, TimeUnit.SECONDS);
        Button purgeAllButton = new Button();
        purgeAllButton.setText("Flush All");
        purgeAllButton.setTextFill(Color.WHITE);
        purgeAllButton.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(5), Insets.EMPTY)));
        purgeAllButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                kad.storedData.flushAll();
            }
        });
        grid.add(purgeAllButton, 3, 5);
        Button purgeRAMButton = new Button();
        purgeRAMButton.setText("Flush RAM");
        purgeRAMButton.setTextFill(Color.WHITE);
        purgeRAMButton.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(5), Insets.EMPTY)));
        purgeRAMButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                kad.storedData.flushRAM();
            }
        });
        grid.add(purgeRAMButton, 0, 5);
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
