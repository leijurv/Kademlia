/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import kademlia.gui.DebugGUITab;

/**
 *
 * @author aidan
 */
public class console {

    public static void log(Object message) {
        if (!Kademlia.silent) {
            System.out.println(message);
        }
        if (!Kademlia.noGUI) {
            DebugGUITab.addLog(message.toString());
        }
    }

    public static void log() {
        if (!Kademlia.silent) {
            System.out.println();
        }
        if (!Kademlia.noGUI) {
            DebugGUITab.addLog("");
        }
    }
}
