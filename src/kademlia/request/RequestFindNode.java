/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import kademlia.Connection;
import kademlia.Kademlia;
import kademlia.lookup.Lookup;
import kademlia.Node;
import kademlia.console;

/**
 *
 * @author leijurv
 */
public class RequestFindNode extends Request {
    private final long nodeid;
    private final Lookup lookup;
    public RequestFindNode(Lookup lookup) {
        super((byte) 2);
        this.nodeid = lookup.key;
        this.lookup = lookup;
    }
    protected RequestFindNode(DataInputStream in) throws IOException {
        super(in, (byte) 2);
        this.nodeid = in.readLong();
        this.lookup = null;
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
        out.writeLong(nodeid);
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) throws IOException {
        if (Kademlia.verbose) {
            console.log(kademliaRef.myself + " Looking for node " + nodeid);
        }
        ArrayList<Node> nodes = kademliaRef.findNClosest(Kademlia.k, nodeid);
        if (Kademlia.verbose) {
            console.log(kademliaRef.myself + " Found " + nodes);
        }
        out.writeInt(nodes.size());
        for (Node n : nodes) {
            n.write(out);
        }
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
        int num = in.readInt();
        ArrayList<Node> nodes = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            nodes.add(new Node(in));
        }
        if (Kademlia.verbose) {
            console.log(conn.kademliaRef.myself + " Got find node resp for search query " + nodeid + ": " + nodes);
        }
        for (Node node : nodes) {
            conn.kademliaRef.addOrUpdate(node);
        }
        Kademlia.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                lookup.foundNodes(nodes);
            }
        });
    }
    @Override
    public void onError0(Connection conn) {
        lookup.onConnectionError();
    }
}
