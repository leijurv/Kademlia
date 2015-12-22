/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class Connection {
    public final Node node;
    public final Socket socket;
    public final DataInputStream in;
    public final DataOutputStream out;
    public static SecureRandom sc = new SecureRandom();
    public final HashMap<Long, Request> pendingRequests;
    public final Kademlia kademliaRef;
    private final Object outLock = new Object();
    public boolean isStillRunning = true;
    public Connection(Node node, Socket socket, Kademlia kademlia) throws IOException {
        this.node = node;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.pendingRequests = new HashMap<>();
        this.kademliaRef = kademlia;
    }
    public void doListen() throws IOException {
        new Thread() {
            private final Random rand = new Random();
            @Override
            public void run() {
                try {
                    while (isStillRunning) {
                        RequestPing rp = new RequestPing();
                        if (!sendRequest(rp)) {
                            console.log("SEND FAILED. PING FAILED.");
                            isStillRunning = false;
                            socket.close();
                            return;
                        }
                        Thread.sleep(10000);
                        if (!isStillRunning || isRequestStillPending(rp)) {
                            console.log("TOOK MORE THAN TEN SECONDS TO RESPOND. PING FAILED.");
                            isStillRunning = false;
                            socket.close();
                            return;
                        }
                        Thread.sleep(60000 + rand.nextInt(5000));//randomness. dont both ping each other at the exact same time.
                    }
                } catch (InterruptedException | IOException ex) {
                    Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
        while (true) {
            readMessage();
        }
    }
    public boolean isRequestStillPending(Request r) {
        return pendingRequests.get(r.requestID) != null;
    }
    public boolean sendRequest(Request r) {
        if (!isStillRunning) {
            throw new IllegalStateException("ur high");
        }
        pendingRequests.put(r.requestID, r);
        try {
            ByteArrayOutputStream temp = new ByteArrayOutputStream();
            r.send(new DataOutputStream(temp));
            byte[] toWrite = temp.toByteArray();
            synchronized (outLock) {
                out.write(toWrite);
            }
            if (Kademlia.verbose) {
                console.log(kademliaRef.myself + " Sent request " + r + " to " + node);
            }
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
            console.log("Exception while sending request");
            pendingRequests.remove(r.requestID);
            return false;
        }
    }
    private void readMessage() throws IOException {
        boolean isResp = in.readBoolean();
        if (isResp) {
            long requestID = in.readLong();
            Request r = pendingRequests.remove(requestID);
            if (r == null) {
                throw new IOException("Sent response ID " + requestID + " for nonexistant request");
            }
            if (Kademlia.verbose) {
                console.log(kademliaRef.myself + " Got response for " + r + " with " + pendingRequests.keySet().size() + " left");
            }
            r.onResponse(in, this);
        } else {
            final Request request = Request.read(in);
            new Thread() {
                @Override
                public void run() {
                    try {
                        ByteArrayOutputStream o = new ByteArrayOutputStream();
                        DataOutputStream tempOut = new DataOutputStream(o);
                        tempOut.writeBoolean(true);
                        tempOut.writeLong(request.requestID);
                        request.execute(kademliaRef, tempOut);
                        byte[] toSend = o.toByteArray();
                        synchronized (outLock) {
                            out.write(toSend);
                        }
                        if (Kademlia.verbose) {
                            console.log(kademliaRef.myself + " Sent response to " + request);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
            if (Kademlia.verbose) {
                console.log(kademliaRef.myself + " Received request " + request + " from " + node);
            }
        }
    }
    @Override
    public String toString() {
        return "CONN " + node + " " + socket;
    }
}
