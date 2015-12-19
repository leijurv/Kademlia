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
    public RequestStore(long key, byte[] value) {
        super((byte) 1);
        this.key = key;
        this.value = value;
    }
    protected RequestStore(long requestID, DataInputStream in) throws IOException {
        super(requestID, (byte) 1);
        this.key = in.readLong();
        int len = in.readInt();
        value = new byte[len];
        in.readFully(value);
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
        out.writeLong(key);
        out.writeInt(value.length);
        out.write(value);
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) {
        System.out.println(kademliaRef.myself + " Executing store request for key " + key + " and data with len " + value.length + " and value hash " + Lookup.hash(value));
        kademliaRef.storedData.put(key, value);
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
    }
}
