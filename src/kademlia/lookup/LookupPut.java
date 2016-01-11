/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.lookup;

import kademlia.request.RequestStore;
import kademlia.gui.DataGUITab;
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
public class LookupPut extends Lookup {
    private final int contOffset;
    private final int contLen;
    private final byte[] contentsToPut;
    private final long lastMod;
    public LookupPut(long key, Kademlia kademliaRef, byte[] contents, long lastModified) {
        this(key, kademliaRef, contents, lastModified, 0, contents.length);
    }
    public LookupPut(long key, Kademlia kademliaRef, byte[] contents, long lastModified, int offset, int length) {
        super(key, kademliaRef, false);
        this.contOffset = offset;
        this.contLen = length;
        this.contentsToPut = contents;
        this.lastMod = lastModified;
    }
    private void putLocally() {
        byte[] temp;
        if (contOffset == 0 && contLen == contentsToPut.length) {
            temp = contentsToPut;
        } else {
            temp = new byte[contLen];
            System.arraycopy(contentsToPut, contOffset, temp, 0, contLen);
        }
        kademliaRef.storedData.put(key, temp, lastMod);
        console.log("done, stored locally");
    }
    @Override
    protected void onNodeLookupCompleted0() {
        if (closest.isEmpty()) {
            putLocally();
            return;
        }
        for (Node storageNode : closest) {
            if (kademliaRef.myself.equals(storageNode)) {
                putLocally();
            } else {
                try {
                    kademliaRef.getOrCreateConnectionToNode(storageNode).sendRequest(new RequestStore(key, contentsToPut, lastMod, contOffset, contLen));
                    console.log("done, stored on " + storageNode);
                } catch (IOException ex) {
                    Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        console.log("done, stored on all " + closest.size());
        if (kademliaRef.max != 0) {
            kademliaRef.progress++;
            if (kademliaRef.progress == kademliaRef.max) {
                console.log("All done storing");
                kademliaRef.max = 0;
                if (!Kademlia.noGUI) {
                    DataGUITab.updateProgressBar(1);
                }
            } else {
                final float progressPercentage = ((float) (kademliaRef.progress)) / ((float) (kademliaRef.max));
                if (!Kademlia.noGUI) {
                    DataGUITab.updateProgressBar(progressPercentage);
                }
                final int width = 50; // progress bar width in chars
                System.out.print("\r[");
                int i = 0;
                for (; i <= (int) (progressPercentage * width); i++) {
                    System.out.print(".");
                }
                for (; i < width; i++) {
                    System.out.print(" ");
                }
                System.out.print("]");
            }
        }
    }
    @Override
    protected void onCompletion() {
        console.log("yay put is done");
    }
}
