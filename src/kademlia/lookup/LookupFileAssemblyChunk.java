/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import kademlia.DDT;
import kademlia.FileAssembly;
import kademlia.Kademlia;

/**
 *
 * @author leijurv
 */
public class LookupFileAssemblyChunk extends Lookup {
    private final FileAssembly assembly;
    public LookupFileAssemblyChunk(long key, Kademlia kademliaRef, FileAssembly assembly) {
        super(key, kademliaRef, true);
        this.assembly = assembly;
        if (DDT.getFromKey(key) != DDT.CHUNK) {
            throw new IllegalArgumentException("lolwut");
        }
    }
    @Override
    protected void onNodeLookupCompleted0() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    @Override
    protected void onCompletion() {
        assembly.onPartCompleted(key, value, true);
    }
}
