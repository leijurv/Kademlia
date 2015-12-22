/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.stage.Stage;

/**
 *
 * @author aidan
 */
public class ConnectionGUITab extends Tab {
    private static Kademlia kad;
    private static Circle selfCircle;
    private static HashMap<Long, Circle> connectionsCircle;
    private static Pane canvas;
    ConnectionGUITab(Stage primaryStage, Kademlia kademliaRef) {
        super();
        
        kad = kademliaRef;
        this.setText("Connection");
        this.setClosable(false);
        
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.TOP_CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        //grid.setGridLinesVisible(true);
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
        connectionsCircle = new HashMap<>();
        
        /* Show Connected Nodes*/
        //Canvas
        /*Canvas nodesCanvas = new Canvas(300, 300);
        GraphicsContext ctx = nodesCanvas.getGraphicsContext2D();
        ctx.setFill(Color.BLUE);
        //ctx.fillRect(0,0,nodesCanvas.getWidth(),nodesCanvas.getHeight());
        ctx.fillOval(nodesCanvas.getWidth()/2, nodesCanvas.getHeight()/2, 30, 30);
        nodesCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED, 
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                if (Math.sqrt((t.getX() - nodesCanvas.getWidth()/2)*(t.getX() - nodesCanvas.getWidth()/2)+(t.getY() - nodesCanvas.getHeight()/2)*(t.getY() - nodesCanvas.getHeight()/2))<30) {
                    System.out.println("self selected");
                }
            }
        });
        grid.add(nodesCanvas, 0, 0, 3, 3);*/
        canvas = new Pane();
        selfCircle = new Circle(15, Color.BLUE);
        selfCircle.relocate(150, 150);
        selfCircle.addEventHandler(MouseEvent.MOUSE_CLICKED, 
        new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent t) {
                System.out.println("self selected");
            }
        });
        canvas.getChildren().add(selfCircle);
        grid.add(canvas, 0, 0, 3, 3);

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
    public static void addConnection() {
        Circle circle = new Circle(15, Color.BLUE); 
        for (int i = 0; i < kad.connections.size(); i++) {
            float angle = (float) (2*Math.PI*(i/kad.connections.size()));
            if (!connectionsCircle.containsKey(kad.connections.get(i).node.nodeid)) {
                connectionsCircle.put(kad.connections.get(i).node.nodeid, circle);
            }
            connectionsCircle.get(kad.connections.get(i).node.nodeid).relocate(Math.cos(angle * 100 + selfCircle.getCenterX()), Math.sin(angle * 100 + selfCircle.getCenterY()));
        }
        canvas.getChildren().add(circle);
        /*Path path = new Path();
        path.getElements().add(new MoveTo(selfCircle.getCenterX(), selfCircle.getCenterY()));
        path.getElements().add(new LineTo(circle.getCenterX(), circle.getCenterY()));*/

    }
}
