/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import kademlia.DDT;
import kademlia.FileAssembly;
import kademlia.Kademlia;
import kademlia.console;

/**
 *
 * @author leijurv
 */
public class LookupAssemblyMetadata extends Lookup {
    private final String storageLocation;
    public LookupAssemblyMetadata(long key, Kademlia kademliaRef, String storageLocation) {
        super(key, kademliaRef, true);
        this.storageLocation = storageLocation;
        if (DDT.getFromKey(key) != DDT.FILE_METADATA) {
            throw new IllegalArgumentException("lolwut");
        }
    }
    @Override
    protected void onCompletion() {
        console.log("Received metadata. Starting assembly...");
        Kademlia.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    new FileAssembly(value, kademliaRef, storageLocation).assemble();
                } catch (IOException ex) {
                    Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
    }
    @Override
    protected void onNodeLookupCompleted0() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
