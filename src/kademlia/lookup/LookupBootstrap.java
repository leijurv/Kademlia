/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import kademlia.Kademlia;
import kademlia.Node;
import kademlia.console;

/**
 *
 * @author leijurv
 */
public class LookupBootstrap extends Lookup {
    public LookupBootstrap(Kademlia kademliaRef) {
        super(kademliaRef.myself.nodeid, kademliaRef, false);
    }
    @Override
    protected void onNodeLookupCompleted0() {
        for (Node node : closest) {
            if (node.nodeid != kademliaRef.myself.nodeid) {
                console.log("bootstrapping to " + node);
                try {
                    kademliaRef.getOrCreateConnectionToNode(node);
                } catch (IOException ex) {
                    Logger.getLogger(LookupBootstrap.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    @Override
    protected void onCompletion() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
