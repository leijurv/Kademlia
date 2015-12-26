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
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final HashMap<Long, Request> pendingRequests;
    public final Kademlia kademliaRef;
    private final Object outLock = new Object();
    private volatile boolean isStillRunning = true;
    public Connection(Node node, Socket socket, Kademlia kademlia) throws IOException {
        this.node = node;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.pendingRequests = new HashMap<>();
        this.kademliaRef = kademlia;
    }
    public void doListen() throws IOException {
        Kademlia.threadPool.execute(new Runnable() {
            private final Random rand = new Random();
            @Override
            public void run() {
                try {
                    while (isStillRunning) {
                        if (!sendRequest(new RequestPing())) {
                            console.log("SEND FAILED. PING FAILED.");
                            Connection.this.close();
                            return;
                        }
                        Thread.sleep(kademliaRef.settings.pingIntervalSec * 1000 + rand.nextInt(kademliaRef.settings.pingIntervalSec * 100));//randomness. dont both ping each other at the exact same time.
                    }
                } catch (InterruptedException ex) {
                    Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        while (true) {
            readMessage();
        }
    }
    public boolean isRequestStillPending(Request r) {
        return pendingRequests.get(r.requestID) != null;
    }
    public boolean sendRequest(Request r) {
        if (!isStillRunning) {
            r.onError(this);
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
            r.sendDate = System.currentTimeMillis();
            if (Kademlia.verbose) {
                console.log(kademliaRef.myself + " Sent request " + r + " to " + node);
            }
            Kademlia.threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(kademliaRef.settings.pingTimeoutSec * 1000);
                        if (isRequestStillPending(r)) {
                            console.log(this + " TOOK MORE THAN " + kademliaRef.settings.pingTimeoutSec + " SECONDS TO RESPOND TO" + r + ". CLOSING CONNECTION.");
                            Connection.this.close();
                            //dont call r.onError here because closing the connection will do it and we dont want duplicate calls
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
            return true;
        } catch (Exception ex) {//yes, catch ALL exceptions. no matter what the exception is, we need this catch to run.
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
            console.log("Exception while sending request " + r);
            r.onError(this);
            //technically I'm pretty sure that an IOException here means that the entire connection is closed...
            //TODO
            pendingRequests.remove(r.requestID);
            return false;
        }
    }
    public boolean isStillRunning() {
        return isStillRunning;
    }
    public void close() {
        console.log(".close was called on " + this);
        kademliaRef.connections.remove(this);//MUY MUY importante
        if (!isStillRunning) {
            console.log("im already closed, leave me alone " + this);
            return;
        }
        isStillRunning = false;
        try {
            socket.close();//this will trigger a cascade of IOExceptions. MUAHAHAHAHAHHA
        } catch (IOException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
        }
        for (long requestid : pendingRequests.keySet()) {
            pendingRequests.get(requestid).onError(this);
        }
        pendingRequests.clear();//this is actually fairly crucial
        //if this weren't here we would have hella memory leaks
        //because some of the requests contain references to lookup objects / storeddata objects
        if (!Kademlia.noGUI) {
            ConnectionGUITab.stoppedConnection(node.nodeid);//LESS LESS importante
        }
    }
    private void readMessage() throws IOException {
        boolean isResp = in.readBoolean();
        if (isResp) {
            long requestID = in.readLong();
            Request r = pendingRequests.remove(requestID);
            if (r == null) {
                throw new IOException("Received response ID " + requestID + " for nonexistant request");
            }
            r.responseDate = System.currentTimeMillis();
            if (Kademlia.verbose) {
                console.log(kademliaRef.myself + " Got response for " + r + " with " + pendingRequests.keySet().size() + " left after " + (r.responseDate - r.sendDate) + "ms");
            }
            r.onResponse(in, this);
        } else {
            final Request request = Request.read(in);
            Kademlia.threadPool.execute(new Runnable() {
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
                        //dont call r.onError because this is the server side lol
                    }
                }
            });
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
