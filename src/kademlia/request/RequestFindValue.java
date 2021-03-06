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
public class RequestFindValue extends Request {
    private final long key;
    private final Lookup lookup;
    public RequestFindValue(Lookup lookup) {
        super((byte) 3);
        this.key = lookup.key;
        this.lookup = lookup;
    }
    protected RequestFindValue(DataInputStream in) throws IOException {
        super(in, (byte) 3);
        this.key = in.readLong();
        this.lookup = null;
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
        out.writeLong(key);
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) throws IOException {
        byte[] value = kademliaRef.storedData.get(key);
        out.writeBoolean(value != null);
        if (value != null) {
            if (Kademlia.verbose) {
                console.log(kademliaRef.myself + " Sending result to search for key " + key);
            }
            out.writeInt(value.length);
            out.write(value);
            return;
        }
        if (Kademlia.verbose) {
            console.log(kademliaRef.myself + " Looking for nodes near key " + key);
        }
        ArrayList<Node> nodes = kademliaRef.findNClosest(Kademlia.k, key);
        if (Kademlia.verbose) {
            console.log(kademliaRef.myself + " Nodes near " + key + " are " + nodes);
        }
        out.writeInt(nodes.size());
        for (Node n : nodes) {
            n.write(out);
        }
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
        boolean hasValue = in.readBoolean();
        if (hasValue) {
            int len = in.readInt();
            byte[] value = new byte[len];
            in.readFully(value);
            Kademlia.threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    lookup.foundValue(value);
                }
            });
            return;
        }
        int num = in.readInt();
        ArrayList<Node> nodes = new ArrayList<>(num);
        for (int i = 0; i < num; i++) {
            nodes.add(new Node(in));
        }
        if (!hasValue) {//be quiet if just found result
            if (Kademlia.verbose) {
                console.log(conn.kademliaRef.myself + " Got find value resp for search query " + key + ": " + nodes);
            }
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
