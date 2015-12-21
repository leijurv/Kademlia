/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;


import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class ConnectionGUITab extends Tab {
    private static Kademlia kad;
    ConnectionGUITab(Stage primaryStage, Kademlia kademliaRef) {
        super();
        
        kad = kademliaRef;
        this.setText("Connection");
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
        
        /* Show Connected Nodes*/
        //Canvas
        Canvas nodesCanvas = new Canvas(250,250);
        GraphicsContext ctx = nodesCanvas.getGraphicsContext2D();
        ctx.setFill(Color.BLUE);
        ctx.fillRect(75,75,50,50);
        ctx.fillOval(10, 60, 30, 30);
        grid.add(nodesCanvas, 0, 0, 3, 3);
        
        /* Add Connection */
        //TextField
        TextField hostnameTextField = new TextField();
        hostnameTextField.setPromptText("Hostname");
        grid.add(hostnameTextField, 0, 5, 3, 1);
        //TextField
        TextField portTextField = new TextField();
        portTextField.setPromptText("Port");
        grid.add(portTextField, 3, 5);
        //Button
        Button connectBtn = new Button();
        connectBtn.setText("Connect");
        connectBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (hostnameTextField.getText().length() == 0 || portTextField.getText().length() == 0) {
                    return;
                }
                hostnameTextField.clear();
                portTextField.clear();
            }
        });
        grid.add(connectBtn, 4, 5);
        
        //Tab
        this.setContent(grid);
    }
}
