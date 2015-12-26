/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Lookup {
    public static final int concurrencyLevel = 3;
    final long key;
    private final Kademlia kademliaRef;
    private byte[] value = null;
    private ArrayList<Node> closest;
    private final ArrayList<Node> alreadyAsked = new ArrayList<>();
    private final boolean isKeyLookup;
    private Node finalResult;
    private byte[] contentsToPut;
    private int contOffset;
    private int contLen;
    private boolean needsToAssemble;
    private FileAssembly assembly;
    private String storageLocation;
    private long lastMod;
    private final StoredData storedData;
    private volatile boolean isClosestNormalized = false;
    private final Object lock = new Object();
    private volatile boolean hasDoneStore = false;
    private volatile int numFinishedThreads = 0;
    private Comparator<Node> distanceComparator;
    public static long maskedHash(byte[] o, DDT ddt) {
        return maskedHash(o, 0, o.length, ddt);
    }
    public static long maskedHash(byte[] o, int offset, int length, DDT ddt) {
        return ddt.mask(unmaskedHash(o, offset, length));
    }
    public static long unmaskedHash(byte[] o) {
        console.log("someone's being a naughty nellie and getting an unmasked hash");
        return unmaskedHash(o, 0, o.length);
    }
    private static long unmaskedHash(byte[] o, int offset, int length) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, e);
            return -1;
        }
        md.update(o, offset, length);
        String string = new String(md.digest());
        long h = 1125899906842597L;
        int len = string.length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return Math.abs(h);
    }
    public Lookup(long key, Kademlia kademliaRef, StoredData data) {
        this.distanceComparator = Node.createDistanceComparator(key);
        this.contLen = 0;
        this.contentsToPut = null;
        this.contOffset = 0;
        this.needsToAssemble = false;
        this.assembly = null;
        this.storageLocation = null;
        this.lastMod = 0;
        this.closest = null;
        this.storedData = data;
        this.isKeyLookup = false;
        this.key = key;
        this.kademliaRef = kademliaRef;
    }
    public Lookup(long key, Kademlia kademliaRef, byte[] contents, long lastModified) {
        this(key, kademliaRef, contents, lastModified, 0, contents.length);
    }
    public Lookup(long key, Kademlia kademliaRef, byte[] contents, long lastModified, int offset, int length) {
        this(key, kademliaRef, false);
        this.lastMod = lastModified;
        this.contentsToPut = contents;
        this.contOffset = offset;
        this.contLen = length;
    }
    public Lookup(FileAssembly f, long key, Kademlia kademliaRef) {
        this(key, kademliaRef, true);
        this.assembly = f;
        this.closest = null;
    }
    public Lookup(long key, Kademlia kademliaRef, boolean isKeyLookup, boolean assemble, String storageLocation) {
        this(key, kademliaRef, isKeyLookup);
        this.closest = null;
        this.storageLocation = storageLocation;
        this.needsToAssemble = assemble;
    }
    public Lookup(long key, Kademlia kademliaRef, boolean isKeyLookup) {
        this.distanceComparator = Node.createDistanceComparator(key);
        this.needsToAssemble = false;
        this.assembly = null;
        this.storageLocation = null;
        this.lastMod = 0;
        this.storedData = null;
        this.closest = null;
        this.key = key;
        this.kademliaRef = kademliaRef;
        this.isKeyLookup = isKeyLookup;
    }
    public Node popFirstNonUsed() {
        synchronized (lock) {
            for (Node n : closest) {
                if (!alreadyAsked.contains(n) && !kademliaRef.myself.equals(n)) {
                    return n;
                }
            }
        }
        return null;
    }
    public boolean isLookupFinished() {
        if (isKeyLookup) {
            return value != null || hasDoneStore;
        } else {
            return finalResult != null || hasDoneStore;
        }
    }
    public void execute() {
        for (int i = 0; i < concurrencyLevel; i++) {
            executeStep();
        }
    }
    private void normalizeClosest() {
        synchronized (lock) {
            if (closest == null) {
                if (Kademlia.verbose) {
                    console.log("Starting lookup for " + key + " for the first time");
                }
                closest = kademliaRef.findNClosest(Kademlia.k, key);
            }
            closest.sort(distanceComparator);
            isClosestNormalized = true;
            while (closest.size() > Kademlia.k) {
                Node removed = closest.remove(closest.size() - 1);
                if (Kademlia.verbose) {
                    console.log("Removed from consideration " + removed + " for key " + key);
                }
            }
        }
    }
    private void executeStep() {
        //note: the reason why the synchronized blocks are in such weird/roundabout places is that I really really really don't want any network calls to be in a synchronized block.
        if (isLookupFinished()) {
            console.log("leave me alone im already done");
            return;
        }
        if (!isClosestNormalized) {
            normalizeClosest();
        }
        if (Kademlia.verbose) {
            synchronized (lock) {
                for (Node n : closest) {
                    console.log("lookup is considering: " + n + " " + (n.nodeid ^ key));
                }
                console.log(alreadyAsked);
            }
        }
        Node node;
        synchronized (lock) {
            node = popFirstNonUsed();
            if (node == null) {
                numFinishedThreads++;
                if (numFinishedThreads == concurrencyLevel) {
                    if (!isKeyLookup) {
                        onNodeLookupCompleted();
                    } else {
                        console.log("failed for " + key);
                    }
                } else {
                    if (numFinishedThreads > concurrencyLevel) {
                        throw new IllegalStateException("oh god, another concurrency issue");
                    }
                    console.log("lookup thread " + numFinishedThreads + " of " + concurrencyLevel + " is done");
                }
                return;
            } else {
                alreadyAsked.add(node);
            }
        }
        Kademlia.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                sendRequestToNode(node);
            }
        });
    }
    private void sendRequestToNode(Node node) {
        Connection conn;
        try {
            conn = kademliaRef.getOrCreateConnectionToNode(node);
        } catch (IOException ex) {
            Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
            console.log("unable to establish conneciton to " + node);
            synchronized (lock) {
                closest.remove(node);//haha, do this BEFORE calling onConnectionError and therefore executeStep
                //dont remove from alreadyAsked, we don't want to ask this node again because they are bad
                //this doesn't un-normalize closest
            }
            onConnectionError();
            return;
        }
        if (isKeyLookup) {
            conn.sendRequest(new RequestFindValue(this));
        } else {
            conn.sendRequest(new RequestFindNode(this));
        }
    }
    private void onNodeLookupCompleted() {
        if (hasDoneStore) {
            throw new IllegalStateException("Already done store");
        }
        hasDoneStore = true;
        if (Kademlia.verbose) {
            console.log("The closest to key " + key + " was " + closest.get(0));
        }
        if (storedData != null) {
            for (Node storageNode : closest) {
                if (!kademliaRef.myself.equals(storageNode)) {
                    try {
                        kademliaRef.getOrCreateConnectionToNode(storageNode).sendRequest(new RequestTest(storedData));
                    } catch (IOException ex) {
                        Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
        if (contentsToPut != null) {
            for (Node storageNode : closest) {
                if (kademliaRef.myself.equals(storageNode)) {
                    byte[] temp;
                    if (contOffset == 0 && contLen == contentsToPut.length) {
                        temp = contentsToPut;
                    } else {
                        temp = new byte[contLen];
                        System.arraycopy(contentsToPut, contOffset, temp, 0, contLen);
                    }
                    kademliaRef.storedData.put(key, temp, lastMod);
                    console.log("done, stored locally");
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
    }
    public void foundNodes(ArrayList<Node> nodes) {
        if (isLookupFinished()) {
            return;
        }
        boolean didDiscoverNewNode = false;
        if (isKeyLookup) {
            while (nodes.contains(kademliaRef.myself)) {
                nodes.remove(kademliaRef.myself);
            }
        }
        synchronized (lock) {
            if (!isClosestNormalized) {
                normalizeClosest();
            }
            long worstDistance = closest.isEmpty() ? Long.MAX_VALUE : closest.get(closest.size() - 1).nodeid ^ key;
            for (Node newNode : nodes) {
                if (!closest.contains(newNode) && !alreadyAsked.contains(newNode)) {
                    long distance = newNode.nodeid ^ key;
                    boolean betterDist = distance <= worstDistance;
                    boolean addAnyway = closest.size() < Kademlia.k;
                    if (Kademlia.verbose) {
                        console.log("Lookup for " + key + " has new node " + newNode + " and " + betterDist);
                    }
                    if (betterDist || addAnyway) {
                        closest.add(newNode);
                        didDiscoverNewNode = true;
                        if (!betterDist) {
                            worstDistance = distance;
                        }
                    }
                }
            }
            for (Node closest1 : closest) {
                if (closest1.nodeid == key) {
                    finalResult = closest1;
                    onCompletion();
                    return;
                }
            }
        }
        if (!didDiscoverNewNode) {
            if (!isLookupFinished()) {//dont talk if already found value
                if (Kademlia.verbose) {
                    console.log("Lookup for " + key + " didn't get anything new");
                }
            }
        }
        if (!isLookupFinished()) {
            if (Kademlia.verbose) {
                console.log("Lookup for " + key + " is recursively executing");
            }
            executeStep();
        }
    }
    public void onConnectionError() {
        if (!isLookupFinished()) {
            Kademlia.threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    executeStep();//if there was an error with one of the requests but we aren't done, try try again
                }
            });
        }
    }
    public void foundValue(byte[] value) {
        if (this.value != null) {
            if (Arrays.hashCode(value) == Arrays.hashCode(this.value)) {
                return;
            }
            console.log("there are potentially many versions of this. here's another");
        }
        this.value = value;
        onCompletion();
    }
    public void onCompletion() {
        if (needsToAssemble) {
            console.log("Received metadata. Starting assembly...");
            Kademlia.threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        new FileAssembly(value, kademliaRef, storageLocation).assemble();
                    } catch (IOException ex) {
                        Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            return;
        }
        if (assembly != null) {
            assembly.onPartCompleted(key, value, true);
            return;
        }
        if (!Kademlia.noGUI) {
            DataGUITab.incomingKeyValueData(key, value);
        }
        console.log((isKeyLookup ? new String(value) : finalResult));
    }
}
