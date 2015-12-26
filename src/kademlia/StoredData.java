/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class StoredData {
    final long key;
    final DDT ddt;
    byte[] data;
    long hash;
    long lastModified;
    long lastRetreived;
    long size;
    final Object lock = new Object();
    static final Random rand = new Random();
    final DataStore dataStoreRef;
    public void deleteFromDisk() {
        getFile().delete();
    }
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(key);
        out.writeLong(size);
        out.writeLong(hash);
        out.writeLong(lastModified);
        out.writeLong(lastRetreived);
    }
    public StoredData(DataInputStream in, DataStore dataStoreRef) throws IOException {
        this.dataStoreRef = dataStoreRef;
        this.key = in.readLong();
        this.size = in.readLong();
        this.hash = in.readLong();
        this.lastModified = in.readLong();
        this.lastRetreived = in.readLong();
        this.data = null;
        this.ddt = DDT.getFromMask(key);
        startThread();
    }
    public StoredData(long key, byte[] data, long lastModified, DataStore dataStoreRef) {
        this.dataStoreRef = dataStoreRef;
        this.key = key;
        this.ddt = DDT.getFromMask(key);
        this.size = data.length;
        this.data = data;
        this.hash = Lookup.unmaskedHash(data);
        this.lastModified = lastModified;
        this.lastRetreived = 0;
        startThread();
    }
    public boolean isInRAM() {
        return data != null;
    }
    public byte[] getData() {
        lastRetreived = System.currentTimeMillis();
        return getData0();
    }
    private byte[] getData0() {
        synchronized (lock) {
            if (data != null) {
                return data;
            }
            File save = getFile();
            if (save.exists()) {
                try (DataInputStream in = new DataInputStream(new FileInputStream(save))) {
                    byte[] temp = new byte[(int) size];
                    in.readFully(temp);
                    long checksum = Lookup.unmaskedHash(temp);
                    if (checksum != hash) {
                        String yolo = "Did read from disk. Expected hash: " + hash + ". Real hash: " + checksum + ". DDT: " + ddt;
                        console.log(yolo);
                        if (ddt == DDT.CHUNK) {
                            throw new IllegalStateException(yolo);
                        }
                    }
                    data = temp;
                    console.log("Read " + size + " bytes from disk for key " + key);
                } catch (IOException ex) {
                    Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
                }
                return data;
            } else {
                throw new IllegalStateException("huh?");
            }
        }
    }
    public void update(byte[] newData, long lastModified) {
        synchronized (lock) {
            this.data = newData;
            this.size = newData.length;
            this.hash = Lookup.unmaskedHash(newData);
            this.lastModified = lastModified;
        }
        beginSave();
    }
    public void beginSave() {
        new Thread() {
            @Override
            public void run() {
                doSave();
            }
        }.start();
    }
    public void doSave() {
        synchronized (lock) {
            File save = getFile();
            try (FileOutputStream out = new FileOutputStream(save)) {
                out.write(data);
            } catch (IOException ex) {
                Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public File getFile() {
        return new File(dataStoreRef.dataStoreDir + key);
    }
    private void startThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(60000 + rand.nextInt(10000));
                        new Lookup(key, dataStoreRef.kademliaRef, StoredData.this).execute();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    public void storeCopyIn(Connection conn) {
        conn.sendRequest(new RequestStore(key, getData0(), lastModified));
    }
}
