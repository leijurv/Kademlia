/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import kademlia.ClientSubscription;
import kademlia.Kademlia;
import kademlia.Node;

/**
 *
 * @author leijurv
 */
public class LookupSubscribe extends Lookup {
    public LookupSubscribe(long key, Kademlia kademliaRef) {
        super(key, kademliaRef, false);
    }
    @Override
    protected void onNodeLookupCompleted0() {
        for (Node node : closest) {
            try {
                ClientSubscription clientSub = kademliaRef.getClientSubscriberToNode(node).subscribeTo(key);
                //todo maybe do something here? idk
            } catch (IOException ex) {
                Logger.getLogger(LookupSubscribe.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    @Override
    protected void onCompletion() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
