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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Lookup {
    final long key;
    private final Kademlia kademliaRef;
    private byte[] value = null;
    private ArrayList<Node> closest;
    private final ArrayList<Node> alreadyAsked = new ArrayList<>();
    private final boolean isKeyLookup;
    private Node finalResult;
    private final byte[] contentsToPut;
    private final int contOffset;
    private final int contLen;
    private final boolean needsToAssemble;
    private final FileAssembly assembly;
    private final String storageLocation;
    private final long lastMod;
    private final StoredData storedData;
    public static long hash(byte[] o) {
        return hash(o, 0, o.length);
    }
    public static long hash(byte[] o, int offset, int length) {
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
        this.closest = null;
    }
    public Lookup(long key, Kademlia kademliaRef, byte[] contents, long lastModified, int offset, int length) {
        this(key, kademliaRef, false);
        this.closest = null;
    }
    public Lookup(String path, Kademlia kademliaRef, byte[] contents, long lastModified) {
        this(hash(path.getBytes()), kademliaRef, contents, lastModified);
        this.closest = null;
    }
    public Lookup(FileAssembly f, long key, Kademlia kademliaRef) {
        this(key, kademliaRef, true);
        this.closest = null;
    }
    public Lookup(String path, Kademlia kademliaRef, boolean isKeyLookup) {
        this(hash(path.getBytes()), kademliaRef, isKeyLookup);
        this.closest = null;
    }
    public Lookup(String path, Kademlia kademliaRef, boolean isKeyLookup, boolean assemble, String storageLocation) {
        this(hash(path.getBytes()), kademliaRef, isKeyLookup);
        this.closest = null;
    }
    public Lookup(long key, Kademlia kademliaRef, boolean isKeyLookup) {
        this.contLen = 0;
        this.contentsToPut = null;
        this.contOffset = 0;
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
        for (Node n : closest) {
            if (!alreadyAsked.contains(n) && !kademliaRef.myself.equals(n)) {
                return n;
            }
        }
        return null;
    }
    public boolean isLookupFinished() {
        if (isKeyLookup) {
            return value != null;
        } else {
            return finalResult != null;
        }
    }
    public void execute() {
        if (closest == null) {
            if (Kademlia.verbose) {
                console.log("Starting lookup for " + key + " for the first time");
            }
            closest = kademliaRef.findNClosest(Kademlia.k, key);
        }
        closest.sort((Node o1, Node o2) -> new Long(o1.nodeid ^ key).compareTo(o2.nodeid ^ key));
        while (closest.size() > Kademlia.k) {
            Node removed = closest.remove(closest.size() - 1);
            if (Kademlia.verbose) {
                console.log("Removed from consideration " + removed + " for key " + key);
            }
        }
        for (Node n : closest) {
            if (Kademlia.verbose) {
                console.log("lookup is considering: " + n + " " + (n.nodeid ^ key));
            }
        }
        if (Kademlia.verbose) {
            console.log(alreadyAsked);
        }
        Node node = popFirstNonUsed();
        if (node == null) {
            if (Kademlia.verbose) {
                console.log("Lookup for " + key + " has failed");
            }
            if (!isKeyLookup) {
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
                            byte[] temp = new byte[contLen - contOffset];
                            for (int i = 0; i < temp.length; i++) {
                                temp[i] = contentsToPut[i + contOffset];
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
                            DataGUITab.updateProgressBar(1);
                        } else {
                            final float progressPercentage = ((float) (kademliaRef.progress)) / ((float) (kademliaRef.max));
                            DataGUITab.updateProgressBar(progressPercentage);
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
            } else {
                console.log("failed");
            }
            return;
        }
        alreadyAsked.add(node);
        Connection conn;
        try {
            conn = kademliaRef.getOrCreateConnectionToNode(node);
        } catch (IOException ex) {
            Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException("unable to establish conneciton to " + node, ex);
        }
        if (isKeyLookup) {
            conn.sendRequest(new RequestFindValue(this));
        } else {
            conn.sendRequest(new RequestFindNode(this));
        }
    }
    public void foundNodes(ArrayList<Node> nodes) {
        if (isLookupFinished()) {
            return;
        }
        boolean didDiscoverNewNode = false;
        long worstDistance = closest.isEmpty() ? Long.MAX_VALUE : closest.get(closest.size() - 1).nodeid ^ key;
        if (isKeyLookup) {
            while (nodes.contains(kademliaRef.myself)) {
                nodes.remove(kademliaRef.myself);
            }
        }
        for (Node newNode : nodes) {
            if (!closest.contains(newNode) && !alreadyAsked.contains(newNode)) {
                long distance = newNode.nodeid ^ key;
                boolean shouldAdd = distance <= worstDistance;
                if (Kademlia.verbose) {
                    console.log("Lookup for " + key + " has new node " + newNode + " and " + shouldAdd);
                }
                if (shouldAdd) {
                    closest.add(newNode);
                    didDiscoverNewNode = true;
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
            execute();
        }
    }
    public void onConnectionError() {
        if (!isLookupFinished()) {
            execute();//if there was an error with one of the requests but we aren't done, try try again
        }
    }
    public void foundValue(byte[] value) {
        this.value = value;
        onCompletion();
    }
    public void onCompletion() {
        if (needsToAssemble) {
            console.log("Received metadata. Starting assembly...");
            new Thread() {
                @Override
                public void run() {
                    try {
                        new FileAssembly(value, kademliaRef, storageLocation).assemble();
                    } catch (IOException ex) {
                        Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
            return;
        }
        if (assembly != null) {
            assembly.onPartCompleted(key, value, true);
            return;
        }
        DataGUITab.incomingKeyValueData(key, value);
        console.log((isKeyLookup ? new String(value) : finalResult));
    }
}
