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
            @Override
            public void run() {
                if (true) {
                    return;
                }
                Random r = new Random();
                try {
                    while (isStillRunning) {
                        RequestPing rp = new RequestPing();
                        if (!sendRequest(rp)) {
                            System.out.println("SEND FAILED. PING FAILED.");
                            isStillRunning = false;
                            socket.close();
                            return;
                        }
                        Thread.sleep(1000);
                        if (!isStillRunning || isRequestStillPending(rp)) {
                            System.out.println("TOOK MORE THAN ONE SECOND TO RESPOND. PING FAILED.");
                            isStillRunning = false;
                            socket.close();
                            return;
                        }
                        Thread.sleep(60000 + r.nextInt(100));//randomness. dont both ping each other at the exact same time.
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
            synchronized (outLock) {
                r.send(out);
            }
            if (Kademlia.verbose) {
                System.out.println(kademliaRef.myself + " Sent request " + r + " to " + node);
            }
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Exception while sending request");
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
                System.out.println(kademliaRef.myself + " Got response for " + r + " with " + pendingRequests.keySet().size() + " left");
            }
            r.onResponse(in, this);
        } else {
            final Request request = Request.read(in);
            new Thread() {
                @Override
                public void run() {
                    try {
                        ByteArrayOutputStream o = new ByteArrayOutputStream();
                        request.execute(kademliaRef, new DataOutputStream(o));
                        synchronized (outLock) {
                            out.writeBoolean(true);
                            out.writeLong(request.requestID);
                            out.write(o.toByteArray());
                        }
                        if (Kademlia.verbose) {
                            System.out.println(kademliaRef.myself + " Sent response to " + request);
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
            if (Kademlia.verbose) {
                System.out.println(kademliaRef.myself + " Received request " + request + " from " + node);
            }
        }
    }
    @Override
    public String toString() {
        return "CONN " + node + " " + socket;
    }
}
