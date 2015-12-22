/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import javafx.scene.shape.Circle;
import javafx.scene.shape.Path;

/**
 *
 * @author aidan
 */
public class CirclePath {
    public Circle circle;
    public Path path;
    CirclePath(Circle circle, Path path) {
        this.circle = circle;
        this.path = path;
    }
}
