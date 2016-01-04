/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import kademlia.request.RequestTest;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import kademlia.Kademlia;
import kademlia.Node;
import kademlia.StoredData;

/**
 *
 * @author leijurv
 */
public class LookupTestStoredData extends Lookup {
    private final StoredData storedData;
    public LookupTestStoredData(Kademlia kademliaRef, StoredData storedData) {
        super(storedData.key, kademliaRef, false);
        this.storedData = storedData;
    }
    @Override
    protected void onNodeLookupCompleted0() {
        for (Node storageNode : closest) {
            if (!kademliaRef.myself.equals(storageNode)) {
                try {
                    kademliaRef.getOrCreateConnectionToNode(storageNode).sendRequest(new RequestTest(storedData));
                } catch (IOException ex) {
                    Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }
    @Override
    protected void onCompletion() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
