/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import kademlia.request.RequestPing;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Bucket {
    private final int distance;
    final HashMap<Long, Node> nodes;
    private final ArrayList<Node> replacementCache;
    private final Kademlia kademliaRef;
    public Bucket(int distance, Kademlia kademliaRef) {
        this.distance = distance;
        this.nodes = new HashMap<>();
        this.replacementCache = new ArrayList<>();
        this.kademliaRef = kademliaRef;
    }
    public Bucket(int distance, Kademlia kademliaRef, DataInputStream in) throws IOException {
        int numNodes = in.readInt();
        this.distance = distance;
        this.nodes = new HashMap<>();
        this.replacementCache = new ArrayList<>();
        this.kademliaRef = kademliaRef;
        for (int i = 0; i < numNodes; i++) {
            Node node = new Node(in);
            node.hostPortVerified = in.readBoolean();
            nodes.put(node.nodeid, node);
        }
    }
    public void write(DataOutputStream out) throws IOException {
        Set<Long> keySet = nodes.keySet();
        out.writeInt(keySet.size());
        for (long nodeid : keySet) {
            Node node = nodes.get(nodeid);
            node.write(out);
            out.writeBoolean(node.hostPortVerified);
        }
    }
    public boolean addOrUpdate(Node node) {
        long id = node.nodeid;
        if (nodes.containsKey(id)) {
            Node previous = nodes.get(id);
            if (previous.sameHost(node)) {
                return false;
            }
            if (previous.hostPortVerified) {
                throw new IllegalStateException("Trying to replace verified node " + previous + " with new node " + node);
            }
            console.log("(diff host) Replacing " + nodes.get(id) + " with " + node);
            nodes.put(id, node);
            return true;
        } else {
            if (nodes.keySet().size() <= Kademlia.k) {
                nodes.put(id, node);
                return true;
            }
            ArrayList<Long> nodeids = new ArrayList<>(nodes.keySet());
            nodeids.sort(Comparator.comparingLong(nodeid -> nodes.get(nodeid).lastSuccessfulDataTransferDate));
            if (Kademlia.verbose) {
                for (long nodeid : nodeids) {
                    console.log("Node id: " + nodeid + ", nodedata: " + nodes.get(nodeid) + ", lastsuc: " + nodes.get(nodeid).lastSuccessfulDataTransferDate);
                }
            }
            replacementCache.add(node);
            long toRemove = nodeids.get(0);
            pingThatNode(toRemove);
            return true;
        }
    }
    public void pingAll() {
        for (long nodeid : nodes.keySet()) {
            pingThatNode(nodeid);
        }
    }
    public void pingThatNode(long toRemove) {
        Node n = nodes.get(toRemove);
        if (n == null) {
            throw new IllegalStateException("YOUR MOTHER");
        }
        Kademlia.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    RequestPing rp = new RequestPing();
                    Connection conn = kademliaRef.getOrCreateConnectionToNode(n);
                    conn.sendRequest(rp);
                    Thread.sleep(kademliaRef.settings.pingTimeoutSec * 1000);
                    if (conn.isStillRunning() && !conn.isRequestStillPending(rp)) {
                        return;
                    }
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Bucket.class.getName()).log(Level.SEVERE, null, ex);
                }
                console.log("removing " + n + " from bucket because its bad");
                removeNode(n);
            }
        });
    }
    @Override
    public String toString() {
        return "BUCKET" + distance;
    }
    public void removeNode(Node n) {
        if (!nodes.containsKey(n.nodeid)) {
            throw new IllegalArgumentException("I DIDNT EVEN HAVE YOU TO BEGIN WITH");
        }
        if (Kademlia.verbose) {
            console.log("Doing remove for " + n);
        }
        nodes.remove(n.nodeid);
        if (nodes.keySet().size() < Kademlia.k && !replacementCache.isEmpty()) {
            replacementCache.sort(Comparator.comparingLong(node -> node.lastSuccessfulDataTransferDate));
            Node toAdd = replacementCache.get(replacementCache.size() - 1);
            addOrUpdate(toAdd);
        }
    }
}
