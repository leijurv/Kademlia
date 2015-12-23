/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Bucket {
    final int distance;
    final ArrayList<Long> nodeids;
    final HashMap<Long, Node> nodes;
    private final ArrayList<Node> replacementCache;
    final Kademlia kademliaRef;
    public Bucket(int distance, Kademlia kademliaRef) {
        this.distance = distance;
        this.nodes = new HashMap<>();
        this.nodeids = new ArrayList<>();
        this.replacementCache = new ArrayList<>();
        this.kademliaRef = kademliaRef;
    }
    public Bucket(int distance, Kademlia kademliaRef, DataInputStream in) throws IOException {
        int numNodes = in.readInt();
        this.distance = distance;
        this.nodes = new HashMap<>();
        this.nodeids = new ArrayList<>(numNodes);
        this.replacementCache = new ArrayList<>();
        this.kademliaRef = kademliaRef;
        for (int i = 0; i < numNodes; i++) {
            Node node = new Node(in);
            nodeids.add(node.nodeid);
            nodes.put(node.nodeid, node);
        }
    }
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(nodeids.size());
        for (long nodeid : nodeids) {
            Node node = nodes.get(nodeid);
            node.write(out);
        }
    }
    public boolean addOrUpdate(Node node) {
        long id = node.nodeid;
        if (nodeids.contains(id)) {
            Node previous = nodes.get(id);
            if (!previous.sameHost(node)) {
                console.log("(diff host) Replacing " + nodes.get(id) + " with " + node);
            }
            nodes.put(id, node);
            return !previous.sameHost(node);
        } else {
            if (nodeids.size() <= Kademlia.k) {
                nodeids.add(id);
                nodes.put(id, node);
                return true;
            }
            long now = System.currentTimeMillis();
            nodeids.sort((Long o1, Long o2) -> new Long(nodes.get(o1).lastSuccessfulDataTransfer).compareTo(nodes.get(o2).lastSuccessfulDataTransfer));
            for (long nodeid : nodeids) {
                if (Kademlia.verbose) {
                    console.log("Node id: " + nodeid + ", nodedata: " + nodes.get(nodeid) + ", lastsuc: " + nodes.get(nodeid).lastSuccessfulDataTransfer);
                }
            }
            replacementCache.add(node);
            long toRemove = nodeids.get(0);
            pingThatNode(toRemove);
            return true;
        }
    }
    public void pingAll() {
        new Thread() {
            @Override
            public void run() {
                for (long nodeid : nodeids) {
                    pingThatNode(nodeid);
                }
            }
        }.start();
    }
    public void pingThatNode(long toRemove) {
        Node n = nodes.get(toRemove);
        if (n == null) {
            throw new IllegalStateException("YOUR MOTHER");
        }
        new Thread() {
            @Override
            public void run() {
                try {
                    RequestPing rp = new RequestPing();
                    Connection conn = kademliaRef.getOrCreateConnectionToNode(n);
                    conn.sendRequest(rp);
                    Thread.sleep(1000);
                    if (conn.isStillRunning && !conn.isRequestStillPending(rp)) {
                        return;
                    }
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Bucket.class.getName()).log(Level.SEVERE, null, ex);
                }
                if (Kademlia.verbose) {
                    console.log("removing " + n + " from bucket because its bad");
                }
                removeNode(n);
            }
        }.start();
    }
    @Override
    public String toString() {
        return "BUCKET" + distance;
    }
    public void removeNode(Node n) {
        if (!nodeids.contains(n.nodeid)) {
            throw new IllegalArgumentException("I DIDNT EVEN HAVE YOU TO BEGIN WITH");
        }
        if (Kademlia.verbose) {
            console.log("Doing remove for " + n);
        }
        nodeids.remove(n.nodeid);
        nodes.remove(n.nodeid);
        if (nodeids.size() < Kademlia.k && !replacementCache.isEmpty()) {
            replacementCache.sort((Node o1, Node o2) -> new Long(o1.lastSuccessfulDataTransfer).compareTo(o2.lastSuccessfulDataTransfer));
            Node toAdd = replacementCache.get(replacementCache.size() - 1);
            nodeids.add(toAdd.nodeid);
            nodes.put(toAdd.nodeid, toAdd);
            pingThatNode(toAdd.nodeid);
        }
    }
}
