/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import kademlia.DDT;
import kademlia.Kademlia;
import kademlia.console;
import kademlia.gui.DataGUITab;

/**
 *
 * @author leijurv
 */
public class LookupNormalGet extends Lookup {
    public LookupNormalGet(long key, Kademlia kademliaRef) {
        super(key, kademliaRef, true);
        if (DDT.getFromKey(key) != DDT.STANDARD_PUT_GET) {
            throw new IllegalArgumentException("lolwut");
        }
    }
    @Override
    protected void onNodeLookupCompleted0() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected void onCompletion() {
        if (!Kademlia.noGUI) {
            DataGUITab.incomingKeyValueData(key, value);
        }
        console.log((isKeyLookup ? new String(value) : finalResult));
    }
}
