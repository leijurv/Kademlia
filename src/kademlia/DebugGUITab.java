/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import javafx.geometry.Pos;
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
public class DebugGUITab extends Tab {
    private static TextArea console;
    private static String textHolder = ""; 
    DebugGUITab(Stage primaryStage) {
        super();
        
        this.setText("Debug");
        this.setClosable(true);

        
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
        console = new TextArea();
        console.setEditable(false);
        //console.setStyle(".text-area .content {-fx-background-color: black}; ");//-fx-text-fill: yellow;
        console.setText(textHolder);
        
        grid.add(console, 0, 0, 10, 5);
        
        //Tab
        this.setContent(grid);
    }
    public static void addLog(String message) {
        if (console == null) {
            textHolder += message + "\n";
            return;
        }
        console.appendText(message + "\n");
    }
}