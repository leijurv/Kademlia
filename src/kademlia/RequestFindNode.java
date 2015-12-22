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

/**
 *
 * @author leijurv
 */
public class RequestFindNode extends Request {
    final long nodeid;
    final Lookup lookup;
    public RequestFindNode(Lookup lookup) {
        super((byte) 2);
        this.nodeid = lookup.key;
        this.lookup = lookup;
    }
    protected RequestFindNode(long requestID, DataInputStream in) throws IOException {
        super(requestID, (byte) 2);
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
        new Thread() {
            @Override
            public void run() {
                lookup.foundNodes(nodes);
            }
        }.start();
    }
}
