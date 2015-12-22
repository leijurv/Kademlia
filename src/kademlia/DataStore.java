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
    final String dataStoreFile;
    final Random rand;
    final Kademlia kademliaRef;
    public DataStore(String data, Kademlia kademliaRef) {
        this.kademliaRef = kademliaRef;
        this.rand = new Random();
        this.dataStoreFile = System.getProperty("user.home") + "/.kademlia/" + data + "/";
        if (getSaveFile().exists()) {
            readFromSave();
        }
        new File(dataStoreFile).mkdirs();
        startThread();
        startMemoryConvervationThread();
    }

    public class StoredData {
        final long key;
        byte[] data;
        long hash;
        long lastModified;
        long lastRetreived;
        long size;
        final Object lock = new Object();
        public void write(DataOutputStream out) throws IOException {
            out.writeLong(key);
            out.writeLong(size);
            out.writeLong(hash);
            out.writeLong(lastModified);
            out.writeLong(lastRetreived);
        }
        public StoredData(DataInputStream in) throws IOException {
            this.key = in.readLong();
            this.size = in.readLong();
            this.hash = in.readLong();
            this.lastModified = in.readLong();
            this.lastRetreived = in.readLong();
            this.data = null;
            startThread();
        }
        public StoredData(long key, byte[] data, long lastModified) {
            this.key = key;
            this.size = data.length;
            this.data = data;
            this.hash = Lookup.hash(data);
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
                    try (FileInputStream in = new FileInputStream(save)) {
                        int fileSize = in.available();
                        if (fileSize != size) {
                            throw new IllegalStateException("save is wrong size");
                        }
                        byte[] temp = new byte[fileSize];
                        int j = in.read(temp);
                        if (j != fileSize) {
                            throw new IllegalStateException("kush");
                        }
                        data = temp;
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
                this.hash = Lookup.hash(newData);
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
            return new File(dataStoreFile + key);
        }
        private void startThread() {
            new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Thread.sleep(60000 + rand.nextInt(10000));
                            boolean wasNot = data == null;
                            System.out.println("Refreshing " + key + " " + wasNot);
                            kademliaRef.put(key, getData0());
                            if (wasNot) {
                                data = null;//#ConverveMemory
                            }
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DataStore.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
        }
    }
    final HashMap<Long, StoredData> storedData = new HashMap<>();
    final Object lock = new Object();
    volatile boolean shouldSave = true;
    final long maxRamSize = 10 * 1048576;
    private void startMemoryConvervationThread() {
        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        System.out.println("Clearing ram");
                        synchronized (lock) {
                            ArrayList<StoredData> inRAM = storedData.keySet().stream().map(key -> storedData.get(key)).filter(x -> x.isInRAM()).collect(Collectors.toCollection(ArrayList::new));
                            long currentRAMSize = inRAM.stream().mapToLong(x -> x.size).sum();
                            inRAM.sort((StoredData o1, StoredData o2) -> new Long(o1.lastRetreived).compareTo(o2.lastRetreived));
                            System.out.println("Size before: " + currentRAMSize);
                            while (currentRAMSize > maxRamSize) {
                                StoredData sd = inRAM.remove(0);
                                System.out.println("Removed " + sd.size + " bytes");
                                sd.data = null;
                                currentRAMSize -= sd.size;
                            }
                            System.out.println("Size after: " + currentRAMSize);
                            System.out.println("Running gc");
                            System.gc();
                        }
                        Thread.sleep(20000);
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
    public boolean hasKey(long key) {
        return storedData.get(key) != null;
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
        long hash = Lookup.hash(value);
        System.out.println("Executing store request for key " + key + " and data with len " + value.length + " and value hash " + hash + " and last modified " + lastModified);
        synchronized (lock) {
            if (hasKey(key)) {
                StoredData data = storedData.get(key);
                if (data.lastModified > lastModified) {
                    System.out.println("Not overwriting because new last modified " + lastModified + " is before current last modified " + data.lastModified);
                    return;
                }
                if (data.hash == hash) {
                    System.out.println("Not overwriting because hash is the same " + hash);
                    return;
                }
                shouldSave = true;
                data.update(value, lastModified);
                return;
            }
            shouldSave = true;
            StoredData data = new StoredData(key, value, lastModified);
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
                    StoredData data = new StoredData(in);
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
