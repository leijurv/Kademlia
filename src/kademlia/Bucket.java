/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

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
    public boolean addOrUpdate(Node node) {
        long id = node.nodeid;
        if (nodeids.contains(id)) {
            Node previous = nodes.get(id);
            if (!previous.sameHost(node)) {
                System.out.println("(diff host) Replacing " + nodes.get(id) + " with " + node);
            }
            nodes.put(id, node);
            return false;
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
                    System.out.println("Node id: " + nodeid + ", nodedata: " + nodes.get(nodeid) + ", lastsuc: " + nodes.get(nodeid).lastSuccessfulDataTransfer);
                }
            }
            replacementCache.add(node);
            long toRemove = nodeids.get(0);
            pingThatNode(toRemove);
            return true;
        }
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
                    if (!conn.isStillRunning || conn.isRequestStillPending(rp)) {
                        if (Kademlia.verbose) {
                            System.out.println("removing " + n + " from bucket because its bad");
                        }
                        removeNode(n);
                    }
                } catch (IOException | InterruptedException ex) {
                    Logger.getLogger(Bucket.class.getName()).log(Level.SEVERE, null, ex);
                }
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
            System.out.println("Doing remove for " + n);
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