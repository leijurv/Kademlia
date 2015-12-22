/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author leijurv
 */
public class Settings {
    final Kademlia kademliaRef;
    public Settings(Kademlia kademliaRef) {
        this.kademliaRef = kademliaRef;
    }
    public Settings(DataInputStream in, Kademlia kademliaRef) throws IOException {
        this.kademliaRef = kademliaRef;
    }
    public void write(DataOutputStream out) throws IOException {
    }
    public void onChange() {
        kademliaRef.heyYouShouldSaveSoon();
    }
}
