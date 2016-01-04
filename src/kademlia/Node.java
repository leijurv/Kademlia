/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Comparator;

/**
 *
 * @author leijurv
 */
public class Node {
    public final long nodeid;
    public final String host;
    public final int port;
    public volatile long lastSuccessfulDataTransferDate = 0;
    public Node(DataInputStream in) throws IOException {
        this.nodeid = in.readLong();
        this.host = in.readUTF();//todo replace with some other way of representing hostname. maybe use inetaddress or something
        this.port = in.readInt();
    }
    public void write(DataOutputStream out) throws IOException {
        out.writeLong(nodeid);
        out.writeUTF(host);
        out.writeInt(port);
    }
    public Node(long nodeid, String host, int port) {
        this.nodeid = nodeid;
        this.host = host;
        this.port = port;
    }
    @Override
    public String toString() {
        return nodeid + "-" + host + ":" + port;
    }
    public boolean sameHost(Node other) {
        return port == other.port && host.equals(other.host);
    }
    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + (int) (this.nodeid ^ (this.nodeid >>> 32));
        return hash;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Node other = (Node) obj;
        if (this.nodeid != other.nodeid) {
            return false;
        }
        if (!sameHost(other)) {
            throw new IllegalStateException(this + " and " + obj + " are being incorrectly compared as being the same");
        }
        return true;
    }
    public static Comparator<Node> createDistanceComparator(final long key) {
        return Comparator.comparingLong(node -> node.nodeid ^ key);
    }
}
