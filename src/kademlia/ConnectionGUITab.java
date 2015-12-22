/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;


import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
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
    private static HashMap<Long, CirclePath> connectionsCircle;
    private static Pane canvas;
    private static Stage primaryStage;
    ConnectionGUITab(Stage primaryStage, Kademlia kademliaRef) {
        super();
        
        this.primaryStage = primaryStage;
        
        kad = kademliaRef;
        this.setText("Connections");
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
        Tooltip.install(selfCircle, new Tooltip("Current Node"));
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
                try {
                    kad.handleSocket(new Socket(hostnameTextField.getText(), Integer.parseInt(portTextField.getText())));
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, ex.getLocalizedMessage());
                    alert.show();
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
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                //Circle circle = new Circle(10, Color.GREEN); 
                for (int i = 0; i < kad.connections.size(); i++) {
                    float angle = (float) (2.0*Math.PI*(((float) i)/((float)kad.connections.size())));
                    if (!connectionsCircle.containsKey(kad.connections.get(i).node.nodeid)) {
                        Path path = new Path();
                        Circle circle = new Circle(10, Color.GREEN);
                        Tooltip tp = new Tooltip(kad.connections.get(i).node.host + ":" + kad.connections.get(i).node.port);
                        Tooltip.install(circle, tp);

                        /*circle.setOnMouseEntered(new EventHandler<MouseEvent>() {
                            @Override
                            public void handle(MouseEvent t) {
                                javafx.scene.Node node = (javafx.scene.Node) t.getSource();
                                tp.show(node, primaryStage.getX() + t.getSceneX(), primaryStage.getY() + t.getSceneY());
                            }
                        });*/
                        connectionsCircle.put(kad.connections.get(i).node.nodeid, new CirclePath(circle, path));
                        canvas.getChildren().add(connectionsCircle.get(kad.connections.get(i).node.nodeid).circle);
                        canvas.getChildren().add(connectionsCircle.get(kad.connections.get(i).node.nodeid).path);
                    }
                    double xCoord = (Math.cos(angle) * 100 + 165) - 10;
                    double yCoord = (Math.sin(angle) * 100 + 165) - 10;
                    
                    connectionsCircle.get(kad.connections.get(i).node.nodeid).circle.relocate(xCoord, yCoord);
                    connectionsCircle.get(kad.connections.get(i).node.nodeid).path.getElements().clear();
                    connectionsCircle.get(kad.connections.get(i).node.nodeid).path.getElements().add(new MoveTo(165, 165));
                    connectionsCircle.get(kad.connections.get(i).node.nodeid).path.getElements().add(new LineTo(xCoord + 10, yCoord + 10));
                    connectionsCircle.get(kad.connections.get(i).node.nodeid).path.toBack();
                    //connectionsCircle.get(kad.connections.get(i).node.nodeid).relocate(175, 175);
                }
            }
        });
    }
    public static void stoppedConnection(long nodeid) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                connectionsCircle.get(nodeid).circle.setFill(Color.RED);
            }
        });
    }
}
