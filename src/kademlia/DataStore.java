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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author leijurv
 */
public class DataStore {
    final String dataStoreFile;
    final Kademlia kademliaRef;
    public DataStore(Kademlia kademliaRef) {
        this.kademliaRef = kademliaRef;
        this.dataStoreFile = kademliaRef.dataStorageDir;
        if (getSaveFile().exists()) {
            try {
                console.log("datastorage is reading metadata from save " + getSaveFile().getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
            }
            readFromSave();
        }
        new File(dataStoreFile).mkdirs();
        startThread();
        startMemoryConvervationThread();
    }
    private final HashMap<Long, StoredData> storedData = new HashMap<>();
    private final Object lock = new Object();
    private volatile boolean shouldSave = true;
    private void startMemoryConvervationThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(kademliaRef.settings.garbageCollectionIntervalSec * 1000);
                        console.log("Clearing ram");
                        synchronized (lock) {
                            ArrayList<StoredData> inRAM = storedData.keySet().stream().map(key -> storedData.get(key)).filter(x -> x.isInRAM()).collect(Collectors.toCollection(ArrayList::new));
                            long currentRAMSize = inRAM.stream().mapToLong(x -> x.size).sum();
                            inRAM.sort((StoredData o1, StoredData o2) -> new Long(o1.lastRetreived).compareTo(o2.lastRetreived));
                            console.log("Size before: " + currentRAMSize);
                            while (currentRAMSize > kademliaRef.settings.maxRAMSizeBytes) {
                                StoredData sd = inRAM.remove(0);
                                synchronized (sd.lock) {
                                    console.log("Removed " + sd.size + " bytes");
                                    sd.data = null;
                                    currentRAMSize -= sd.size;
                                }
                            }
                            console.log("Size after: " + currentRAMSize);
                        }
                        console.log("Done clearing. Running gc...");
                        System.gc();
                        console.log("Done running gc.");
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
    public byte[] get(long key) {
        StoredData data = storedData.get(key);
        if (data == null) {
            return null;
        }
        shouldSave = true;
        return data.getData();
    }
    public StoredData getMetadata(long key) {
        return storedData.get(key);
    }
    public boolean hasKey(long key) {
        return storedData.get(key) != null;
    }
    public void flushRAM() {
        synchronized (lock) {
            for (long l : storedData.keySet()) {
                StoredData sd = storedData.get(l);
                synchronized (sd.lock) {
                    sd.data = null;
                }
            }
        }
        System.gc();
    }
    public long bytesStoredInRAM() {
        synchronized (lock) {
            return storedData.keySet().stream().map(key -> storedData.get(key)).filter(x -> x.isInRAM()).mapToLong(x -> x.size).sum();
        }
    }
    public long bytesStoredOnDisk() {
        synchronized (lock) {
            return storedData.keySet().stream().map(key -> storedData.get(key)).filter(x -> !x.isInRAM()).mapToLong(x -> x.size).sum();
        }
    }
    public long bytesStoredInTotal() {
        synchronized (lock) {
            return storedData.keySet().stream().mapToLong(key -> storedData.get(key).size).sum();
        }
    }
    public void put(long key, byte[] value, long lastModified) {
        DDT ddt = DDT.getFromMask(key);
        long hash = Lookup.maskedHash(value, DDT.CHUNK);
        if (key != hash && ddt == DDT.CHUNK) {
            console.log("THIS IS BAD BB. DDT IS CHUNK BUT IT DONT MATCH");
        }
        console.log((key != hash ? "BAD HASH" : "GOOD KUSH") + " Executing store request for key " + key + " and data with len " + value.length + " and value hash " + hash + " and last modified " + lastModified + " and DDT " + ddt);
        synchronized (lock) {
            if (hasKey(key)) {
                StoredData data = storedData.get(key);
                if (data.lastModified > lastModified) {
                    console.log("Not overwriting because new last modified " + lastModified + " is before current last modified " + data.lastModified);
                    return;
                }
                if (data.hash == hash) {
                    console.log("Not overwriting because hash is the same " + hash);
                    return;
                }
                console.log("OVERWRITING " + data.key);
                shouldSave = true;
                data.update(value, lastModified);
                return;
            }
            shouldSave = true;
            StoredData data = new StoredData(key, value, lastModified, this);
            storedData.put(key, data);
            data.beginSave();
        }
    }
    private void doSave() {
        synchronized (lock) {
            try (FileOutputStream fileOut = new FileOutputStream(getSaveFile())) {
                DataOutputStream out = new DataOutputStream(fileOut);
                Set<Long> keySet = storedData.keySet();
                out.writeInt(keySet.size());
                for (Long l : keySet) {
                    storedData.get(l).write(out);
                }
            } catch (IOException ex) {
                Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
            }
            shouldSave = false;
        }
    }
    private File getSaveFile() {
        return new File(dataStoreFile + "data");
    }
    private void readFromSave() {
        synchronized (lock) {
            try (FileInputStream fileIn = new FileInputStream(getSaveFile())) {
                DataInputStream in = new DataInputStream(fileIn);
                int num = in.readInt();
                storedData.clear();
                for (int i = 0; i < num; i++) {
                    StoredData data = new StoredData(in, this);
                    storedData.put(data.key, data);
                }
            } catch (IOException ex) {
                Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private void startThread() {
        new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                        if (shouldSave) {
                            doSave();
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }
}
