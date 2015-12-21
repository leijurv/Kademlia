/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 *
 * @author leijurv
 */
public abstract class Request {
    private static final SecureRandom sc = new SecureRandom();
    final byte requestType;
    final long requestID;
    public Request(byte requestType) {
        this.requestType = requestType;
        requestID = sc.nextLong();
    }
    protected Request(long requestID, byte requestType) throws IOException {
        this.requestID = requestID;
        this.requestType = requestType;
    }
    public void send(DataOutputStream out) throws IOException {
        out.writeBoolean(false);//isResp
        out.writeLong(requestID);
        out.writeByte(requestType);
        sendData(out);
    }
    public abstract void sendData(DataOutputStream out) throws IOException;
    public abstract void execute(Kademlia kademliaRef, DataOutputStream out) throws IOException;
    public abstract void onResponse(DataInputStream in, Connection conn) throws IOException;
    public static Request read(DataInputStream in) throws IOException {
        long requestID = in.readLong();
        byte requestType = in.readByte();
        switch (requestType) {
            case 0:
                return new RequestPing(requestID, in);
            case 1:
                return new RequestStore(requestID, in);
            case 2:
                return new RequestFindNode(requestID, in);
            case 3:
                return new RequestFindValue(requestID, in);
            default:
                throw new IOException("Invalid request type " + requestType);
        }
    }
}