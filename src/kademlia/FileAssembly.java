/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.InflaterOutputStream;

/**
 *
 * @author leijurv
 */
public class FileAssembly {
    final long[] hashes;
    final byte[][] parts;
    final Kademlia kademliaRef;
    final String storageLocation;
    public FileAssembly(byte[] header, Kademlia kademliaRef, String storageLocation) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(header));
        int size = in.readInt();
        int partSize = in.readInt();
        int partitions = (int) Math.ceil(((double) size) / ((double) partSize));
        hashes = new long[partitions];
        for (int i = 0; i < partitions; i++) {
            hashes[i] = in.readLong();
        }
        parts = new byte[partitions][];
        this.kademliaRef = kademliaRef;
        this.storageLocation = storageLocation;
    }
    String search = "";
    public void assemble() {
        HashSet<Long> uniqueh = new HashSet<>();
        for (int i = 0; i < hashes.length; i++) {
            uniqueh.add(hashes[i]);
        }
        if (uniqueh.size() != hashes.length) {
            console.log("Of the " + hashes.length + " hashes, there were " + (hashes.length - uniqueh.size()) + " duplicates. Only need to get " + (uniqueh.size()) + " hashes");
        }
        for (long hash : uniqueh) {
            new Thread() {
                @Override
                public void run() {
                    byte[] pos = kademliaRef.storedData.get(hash);
                    if (pos != null) {
                        onPartCompleted(hash, pos, false);
                    } else {
                        search += (hash + "\n");
                        new Lookup(FileAssembly.this, hash, kademliaRef).execute();
                    }
                }
            }.start();
        }
        try {
            Thread.sleep(100);//there is probably a better way to wait for all the threads we just started to append to search
        } catch (InterruptedException ex) {
            Logger.getLogger(FileAssembly.class.getName()).log(Level.SEVERE, null, ex);
        }
        console.log("LOOKING FOR " + search);
    }
    public void onPartCompleted(long key, byte[] contents, boolean t) {
        onPartCompleted1(key, contents, t);
        int numUncompleted = 0;
        for (int i = 0; i < hashes.length; i++) {
            if (parts[i] == null) {
                numUncompleted++;
            }
        }
        DataGUITab.updateProgressBar(((float) (hashes.length - numUncompleted)) / ((float) (hashes.length)));
        console.log(numUncompleted + " parts left");
        if (numUncompleted != 0) {
            return;
        }
        console.log("done getting the file");
        try (OutputStream out = new InflaterOutputStream(new FileOutputStream(new File(storageLocation)))) {
            for (int i = 0; i < hashes.length; i++) {
                out.write(parts[i]);
                //System.out.write(parts[i]);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileAssembly.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void onPartCompleted1(long key, byte[] contents, boolean t) {
        int encounters = 0;
        for (int i = 0; i < hashes.length; i++) {
            if (hashes[i] == key) {
                if (t) {
                    console.log("Received part " + i + ", with hash " + key);
                }
                parts[i] = contents;
                encounters++;
            }
        }
        if (encounters != 1) {
            if (encounters == 0) {
                throw new IllegalArgumentException("shrek");
            }
            if (encounters > 1) {
                console.log("got " + key + " " + encounters + " times. this means that the file has the exact same chunk repeated " + encounters + " times");
            }
        }
    }
}
