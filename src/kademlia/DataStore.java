/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import kademlia.lookup.Lookup;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author leijurv
 */
public class DataStore {
    final String dataStoreDir;
    final Kademlia kademliaRef;
    private final HashMap<Long, StoredData> storedData;
    private final Object lock;
    private volatile boolean shouldSave;
    static final Random rand = new Random();
    public DataStore(Kademlia kademliaRef) {
        this.shouldSave = true;
        this.lock = new Object();
        this.storedData = new HashMap<>();
        this.kademliaRef = kademliaRef;
        this.dataStoreDir = kademliaRef.dataStorageDir;
        if (getSaveFile().exists()) {
            try {
                console.log("datastorage is reading metadata from save " + getSaveFile().getCanonicalPath());
            } catch (IOException ex) {
                Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
            }
            readFromSave();
            shouldSave = false;
        }
        new File(dataStoreDir).mkdirs();
        startSaveThread();
        startMemoryConvervationThread();
        startTestThread();
    }
    private StoredData getRandomStoredData() {
        synchronized (lock) {
            List<Long> keyset = new ArrayList<>(storedData.keySet());
            if (keyset.isEmpty()) {
                return null;
            }
            return storedData.get(keyset.get(rand.nextInt(keyset.size() - 1)));
        }
    }
    private StoredData getFirstStoredData() {
        StoredData best = null;
        long w = Long.MAX_VALUE;
        synchronized (lock) {
            for (long l : storedData.keySet()) {
                StoredData sd = storedData.get(l);
                if (sd.lastTested < w) {
                    w = sd.lastTested;
                    best = sd;
                }
            }
        }
        console.log("test lag: " + (System.currentTimeMillis() - w));
        return best;
    }
    private StoredData whatShouldITest() {
        if (rand.nextInt(10) == 0) {
            return getRandomStoredData();
        }
        return getFirstStoredData();
    }
    private void startTestThread() {
        int fullRefreshInterval = 60000;//TODO make this a setting
        int minwait = 1000;//TODO make this a setting
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        int numStored = storedData.keySet().size();
                        int coefficient = Math.max(numStored, 1);
                        int waitTime = fullRefreshInterval / coefficient;
                        int finalWaitTime = waitTime + rand.nextInt(waitTime / 10) + minwait;
                        int avgWaitTime = waitTime + waitTime / 20 + minwait;
                        int totalWaitTimePredicted = avgWaitTime * coefficient;
                        console.log("wait status: " + numStored + " " + waitTime + " " + finalWaitTime + " " + avgWaitTime + " " + totalWaitTimePredicted);
                        Thread.sleep(finalWaitTime);
                        if (numStored == 0) {
                            continue;
                        }
                        StoredData sd = whatShouldITest();
                        sd.runTest();
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }
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
                            inRAM.sort(Comparator.comparingLong(data -> data.lastRetreived));
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
    public void flushAll() {
        synchronized (lock) {
            for (long l : storedData.keySet()) {
                StoredData sd = storedData.get(l);
                synchronized (sd.lock) {
                    sd.deleteFromDisk();
                    sd.data = null;
                }
            }
            storedData.clear();
            shouldSave = true;
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
    public long itemsStoredInRAM() {
        synchronized (lock) {
            return storedData.keySet().stream().map(key -> storedData.get(key)).filter(x -> x.isInRAM()).count();
        }
    }
    public long itemsStoredOnDisk() {
        synchronized (lock) {
            return storedData.keySet().stream().map(key -> storedData.get(key)).filter(x -> !x.isInRAM()).count();
        }
    }
    public long itemsStoredInTotal() {
        synchronized (lock) {
            return storedData.keySet().size();
        }
    }
    public void put(long key, byte[] value, long lastModified) {
        DDT ddt = DDT.getFromKey(key);
        long hash = Lookup.maskedHash(value, DDT.CHUNK);
        if (key != hash && ddt == DDT.CHUNK) {
            throw new IllegalStateException("THIS IS BAD BB. DDT IS CHUNK BUT IT DONT MATCH");
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
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(getSaveFile())))) {
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
        return new File(dataStoreDir + "data");
    }
    private void readFromSave() {
        synchronized (lock) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(getSaveFile())))) {
                int num = in.readInt();
                StoredData[] data = new StoredData[num];
                for (int i = 0; i < num; i++) {
                    data[i] = new StoredData(in, this);
                }
                storedData.clear();
                for (int i = 0; i < num; i++) {
                    storedData.put(data[i].key, data[i]);//todo: at this point check if the file exists
                }
                //todo: at this point check if there are extra storedData in the data storage folder that we don't have metadata for
                //and if so, delete them
            } catch (IOException ex) {
                Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    private void startSaveThread() {
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
