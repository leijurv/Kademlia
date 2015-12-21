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
    final Kademlia kademliaRef;
    byte[] value = null;
    ArrayList<Node> closest = null;
    ArrayList<Node> alreadyAsked = new ArrayList<>();
    final boolean isKeyLookup;
    Node finalResult;
    byte[] contentsToPut = null;
    boolean needsToAssemble = false;
    FileAssembly assembly = null;
    String storageLocation = null;
    long lastMod = 0;
    static MessageDigest md = null;
    static {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    public static long hash(byte[] o) {
        md.reset();
        String string = new String(md.digest(o));
        long h = 1125899906842597L;
        int len = string.length();
        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return Math.abs(h);
    }
    public Lookup(long key, Kademlia kademliaRef, byte[] contents, long lastModified) {
        this(key, kademliaRef, false);
        contentsToPut = contents;
        this.lastMod = lastModified;
    }
    public Lookup(String path, Kademlia kademliaRef, byte[] contents, long lastModified) {
        this(path, kademliaRef, false);
        contentsToPut = contents;
        this.lastMod = lastModified;
    }
    public Lookup(FileAssembly f, long key, Kademlia kademliaRef) {
        this(key, kademliaRef, true);
        assembly = f;
    }
    public Lookup(String path, Kademlia kademliaRef, boolean isKeyLookup) {
        this(hash(path.getBytes()), kademliaRef, isKeyLookup);
    }
    public Lookup(String path, Kademlia kademliaRef, boolean isKeyLookup, boolean assemble, String storageLocation) {
        this(hash(path.getBytes()), kademliaRef, isKeyLookup);
        this.needsToAssemble = assemble;
        this.storageLocation = storageLocation;
    }
    public Lookup(long key, Kademlia kademliaRef, boolean isKeyLookup) {
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
                System.out.println("Starting lookup for " + key + " for the first time");
            }
            closest = kademliaRef.findNClosest(Kademlia.k, key);
        }
        closest.sort((Node o1, Node o2) -> new Long(o1.nodeid ^ key).compareTo(o2.nodeid ^ key));
        while (closest.size() > Kademlia.k) {
            Node removed = closest.remove(closest.size() - 1);
            if (Kademlia.verbose) {
                System.out.println("Removed from consideration " + removed + " for key " + key);
            }
        }
        for (Node n : closest) {
            if (Kademlia.verbose) {
                System.out.println("lookup is considering: " + n + " " + (n.nodeid ^ key));
            }
        }
        if (Kademlia.verbose) {
            System.out.println(alreadyAsked);
        }
        Node node = popFirstNonUsed();
        if (node == null) {
            if (Kademlia.verbose) {
                System.out.println("Lookup for " + key + " has failed");
            }
            if (!isKeyLookup) {
                if (Kademlia.verbose) {
                    System.out.println("The closest to key " + key + " was " + closest.get(0));
                }
                if (contentsToPut != null) {
                    for (Node storageNode : closest) {
                        if (kademliaRef.myself.equals(storageNode)) {
                            kademliaRef.storedData.put(key, contentsToPut, lastMod);
                            System.out.println("done, stored locally");
                        } else {
                            try {
                                kademliaRef.getOrCreateConnectionToNode(storageNode).sendRequest(new RequestStore(key, contentsToPut, lastMod));
                                System.out.println("done, stored on " + storageNode);
                            } catch (IOException ex) {
                                Logger.getLogger(Lookup.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }
                    System.out.println("done, stored on all " + closest.size());
                    if (kademliaRef.max != 0) {
                        kademliaRef.progress++;
                        if (kademliaRef.progress == kademliaRef.max) {
                            System.out.println("All done storing");
                            kademliaRef.max = 0;
                        } else {
                            final float progressPercentage = ((float) (kademliaRef.progress)) / ((float) (kademliaRef.max));
                            GUI.updateProgressBar(progressPercentage);
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
                System.out.println("failed");
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
                    System.out.println("Lookup for " + key + " has new node " + newNode + " and " + shouldAdd);
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
                    System.out.println("Lookup for " + key + " didn't get anything new");
                }
            }
        }
        if (!isLookupFinished()) {
            if (Kademlia.verbose) {
                System.out.println("Lookup for " + key + " is recursively executing");
            }
            execute();
        }
    }
    public void foundValue(byte[] value) {
        this.value = value;
        onCompletion();
    }
    public void onCompletion() {
        GUI.incomingKeyValueData(key, value);
        if (needsToAssemble) {
            System.out.println("Received metadata. Starting assembly...");
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
        System.out.println((isKeyLookup ? new String(value) : finalResult));
    }
}
