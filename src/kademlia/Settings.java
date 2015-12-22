/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Settings {
    private final Kademlia kademliaRef;
    public volatile long maxRAMSizeBytes;
    public volatile int garbageCollectionIntervalSec;
    public volatile int pingTimeoutSec;
    public volatile int pingIntervalSec;
    public Settings(Kademlia kademliaRef) {
        this.kademliaRef = kademliaRef;
        this.maxRAMSizeBytes = 10 * 1048576;
        this.garbageCollectionIntervalSec = 20;
        this.pingTimeoutSec = 20;
        this.pingIntervalSec = 60;
    }
    public Settings(DataInputStream in, Kademlia kademliaRef) throws IOException {
        this.kademliaRef = kademliaRef;
        this.maxRAMSizeBytes = in.readLong();
        this.garbageCollectionIntervalSec = in.readInt();
        this.pingTimeoutSec = in.readInt();
        this.pingIntervalSec = in.readInt();
    }
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(maxRAMSizeBytes);
        out.writeInt(garbageCollectionIntervalSec);
        out.writeInt(pingTimeoutSec);
        out.writeInt(pingIntervalSec);
    }
    public void onChange() {
        System.out.println("New settings: " + this);
        kademliaRef.heyYouShouldSaveSoon();
    }
    @Override
    public String toString() {
        Field[] allFields = Settings.class.getDeclaredFields();
        String resp = "\n";
        for (Field field : allFields) {
            try {
                if (!field.toString().contains("kademliaRef")) {
                    resp += field + ":" + field.get(this) + "\n";
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return resp;
    }
}
