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
    final int offset;
    final int length;
    public RequestStore(long key, byte[] value, long lastModified, int offset, int length) {
        super((byte) 1);
        this.key = key;
        this.value = value;
        this.lastModified = lastModified;
        this.offset = offset;
        this.length = length;
    }
    public RequestStore(long key, byte[] value, long lastModified) {
        this(key, value, lastModified, 0, value.length);
    }
    protected RequestStore(long requestID, DataInputStream in) throws IOException {
        super(requestID, (byte) 1);
        this.key = in.readLong();
        this.lastModified = in.readLong();
        this.length = in.readInt();
        value = new byte[length];
        in.readFully(value);
        this.offset = 0;
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
        out.writeLong(key);
        out.writeLong(lastModified);
        out.writeInt(length);
        out.write(value, offset, length);
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) {
        kademliaRef.storedData.put(key, value, lastModified);
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
    }
}
