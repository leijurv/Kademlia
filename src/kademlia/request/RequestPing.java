/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import kademlia.Connection;
import kademlia.Kademlia;

/**
 *
 * @author leijurv
 */
public class RequestPing extends Request {
    public RequestPing() {
        super((byte) 0);
    }
    protected RequestPing(DataInputStream in) throws IOException {
        super(in, (byte) 0);
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) {
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
        conn.node.lastSuccessfulDataTransferDate = System.currentTimeMillis();
    }
    @Override
    public void onError0(Connection conn) {
    }
}
