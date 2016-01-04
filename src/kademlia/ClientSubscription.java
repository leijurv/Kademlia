/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author leijurv
 */
public class ClientSubscription {
    private final Socket socket;
    private final Node node;
    private final DataInputStream in;
    private final DataOutputStream out;
    final long key;
    private final ClientSubscriber clientSubscriberRef;
    private final boolean didAskForCurrent = true;
    private final boolean didAskForFull = true;
    ClientSubscription(Node node, Socket socket, DataInputStream in, DataOutputStream out, long key, ClientSubscriber clientSubscriberRef) throws IOException {
        this.node = node;
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.key = key;
        this.clientSubscriberRef = clientSubscriberRef;
    }
    final void sendSubInfo() throws IOException {
        synchronized (clientSubscriberRef.outLock) {
            out.writeByte(0);
            out.writeLong(key);
            out.writeByte(didAskForCurrent ? -182 : 0);//trust me
            out.writeByte(didAskForFull ? 57 : 0);//trust me
        }
    }
    void onError() {//maybe make this abstract?
    }
    protected void newStoredData(StoredData storedData) {//maybe make this abstract?
        console.log("New value for key " + key + " (I got this update from from node " + node + "):");
        console.log(new String(storedData.data));
    }
    final void onSubData() throws IOException {//dont override this
        newStoredData(readSubData());
    }
    private StoredData readSubData() throws IOException {
        boolean wasNull = in.readBoolean();
        if (wasNull) {
            return null;
        }
        StoredData resp = new StoredData(in, null);
        if (resp.key != key) {
            throw new IllegalStateException("no clue " + resp.key + " " + key);
        }
        if (didAskForFull) {
            resp.data = new byte[resp.size];
            in.readFully(resp.data);
        }
        return resp;
    }
}
