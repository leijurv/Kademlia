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
import java.util.logging.Level;
import java.util.logging.Logger;

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
        for (int i = 0; i < hashes.length; i++) {
            final int j = i;
            new Thread() {
                @Override
                public void run() {
                    byte[] pos = kademliaRef.storedData.get(hashes[j]);
                    if (pos != null) {
                        System.out.println("Part " + j + " was stored locally");
                        onPartCompleted(hashes[j], pos, false);
                    } else {
                        search += hashes[j];
                        search += "\n";
                        new Lookup(FileAssembly.this, hashes[j], kademliaRef).execute();
                    }
                }
            }.start();
        }
        System.out.println("LOOKING FOR " + search);
    }
    public void onPartCompleted(long key, byte[] contents, boolean t) {
        onPartCompleted1(key, contents, t);
        int numUncompleted = 0;
        for (int i = 0; i < hashes.length; i++) {
            if (parts[i] == null) {
                numUncompleted++;
            }
        }
        System.out.println(numUncompleted + " parts left");
        if (numUncompleted != 0) {
            return;
        }
        System.out.println("done getting the file");
        try (FileOutputStream out = new FileOutputStream(new File(storageLocation))) {
            for (int i = 0; i < hashes.length; i++) {
                out.write(parts[i]);
                //System.out.write(parts[i]);
            }
        } catch (IOException ex) {
            Logger.getLogger(FileAssembly.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void onPartCompleted1(long key, byte[] contents, boolean t) {
        boolean encountered = false;
        for (int i = 0; i < hashes.length; i++) {
            if (hashes[i] == key) {
                if (t) {
                    System.out.println("Received part " + i + ", with hash " + key);
                }
                parts[i] = contents;
                if (encountered) {
                    System.out.println("got " + key + " more than once");
                }
                encountered = true;
            }
        }
        if (!encountered) {
            throw new IllegalArgumentException("shrek");
        }
    }
}
