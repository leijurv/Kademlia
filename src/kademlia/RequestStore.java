/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author leijurv
 */
public class RequestStore extends Request {
    final long key;
    final byte[] value;
    final long lastModified;
    public RequestStore(long key, byte[] value, long lastModified) {
        super((byte) 1);
        this.key = key;
        this.value = value;
        this.lastModified = lastModified;
    }
    protected RequestStore(long requestID, DataInputStream in) throws IOException {
        super(requestID, (byte) 1);
        this.key = in.readLong();
        this.lastModified = in.readLong();
        int len = in.readInt();
        value = new byte[len];
        in.readFully(value);
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
        out.writeLong(key);
        out.writeLong(lastModified);
        out.writeInt(value.length);
        out.write(value);
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) {
        kademliaRef.storedData.put(key, value, lastModified);
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
    }
}
