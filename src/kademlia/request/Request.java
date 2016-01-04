/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import kademlia.Connection;
import kademlia.Kademlia;

/**
 *
 * @author leijurv
 */
public abstract class Request {
    private static final SecureRandom sc = new SecureRandom();
    final byte requestType;
    public final long requestID;
    public volatile long sendDate = -1;
    public volatile long responseDate = -1;
    private volatile boolean hasErrored = false;
    private final Object hasErroredLock = new Object();
    protected Request(byte requestType) {
        this.requestType = requestType;
        requestID = sc.nextLong();
    }
    protected Request(DataInputStream in, byte requestType) throws IOException {
        this.requestID = in.readLong();
        this.requestType = requestType;
    }
    public void send(DataOutputStream out) throws IOException {
        out.writeBoolean(false);//isResp
        out.writeByte(requestType);
        out.writeLong(requestID);
        sendData(out);
    }
    public abstract void sendData(DataOutputStream out) throws IOException;
    public abstract void execute(Kademlia kademliaRef, DataOutputStream out) throws IOException;
    public abstract void onResponse(DataInputStream in, Connection conn) throws IOException;
    protected abstract void onError0(Connection conn);
    public void onError(Connection conn) {//we really can't have dupes of this
        synchronized (hasErroredLock) {//we really really really can't have dupes of this
            //is a object lock overkill for a boolean thats already volatile? maybe...
            //maybe.
            if (hasErrored) {
                throw new IllegalStateException("I have already errored. " + this);
            }
            hasErrored = true;
        }
        onError0(conn);
    }
    public static Request read(DataInputStream in) throws IOException {
        byte requestType = in.readByte();
        switch (requestType) {
            case 0:
                return new RequestPing(in);
            case 1:
                return new RequestStore(in);
            case 2:
                return new RequestFindNode(in);
            case 3:
                return new RequestFindValue(in);
            case 4:
                return new RequestTest(in);
            default:
                throw new IOException("Invalid request type " + requestType);
        }
    }
}
