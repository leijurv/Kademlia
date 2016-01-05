/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import kademlia.request.Request;
import kademlia.request.RequestPing;
import kademlia.gui.ConnectionGUITab;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

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
    private final SecureRandom rand = new SecureRandom();
    public Connection(Node node, Socket socket, Kademlia kademlia) throws IOException {
        this.node = node;
        this.socket = socket;
        ECPoint sharedPoint = kademlia.getSharedSecret(node);
        byte[] myTempData = new byte[32];
        rand.nextBytes(myTempData);
        socket.getOutputStream().write(myTempData);
        byte[] theirTempData = new byte[32];
        new DataInputStream(socket.getInputStream()).readFully(theirTempData);
        ByteArrayOutputStream sharedIn = new ByteArrayOutputStream();
        DataOutputStream sin = new DataOutputStream(sharedIn);
        ByteArrayOutputStream sharedOut = new ByteArrayOutputStream();
        DataOutputStream sout = new DataOutputStream(sharedOut);
        sharedPoint.write(sin);
        sharedPoint.write(sout);
        sout.write(theirTempData);
        sout.write(myTempData);
        sin.write(myTempData);
        sin.write(theirTempData);
        byte[] sharedIN = sharedIn.toByteArray();
        byte[] sharedOUT = sharedOut.toByteArray();
        System.out.println("Shared secret IN: " + Arrays.hashCode(sharedIN));
        System.out.println("Shared secret OUT: " + Arrays.hashCode(sharedOUT));
        System.out.println("LENGTH: " + sharedIN.length);
        try {
            Cipher rc4Encrypt = Cipher.getInstance("RC4");
            SecretKeySpec rc4KeyEncrypt = new SecretKeySpec(sharedOUT, "RC4");
            rc4Encrypt.init(Cipher.ENCRYPT_MODE, rc4KeyEncrypt);
            Cipher rc4Decrypt = Cipher.getInstance("RC4");
            SecretKeySpec rc4KeyDecrypt = new SecretKeySpec(sharedIN, "RC4");
            rc4Decrypt.init(Cipher.DECRYPT_MODE, rc4KeyDecrypt);
            this.in = new DataInputStream(new CipherInputStream(socket.getInputStream(), rc4Decrypt));
            this.out = new DataOutputStream(new CipherOutputStream(socket.getOutputStream(), rc4Encrypt));
            this.pendingRequests = new HashMap<>();
            this.kademliaRef = kademlia;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException ex) {
            Logger.getLogger(Connection.class.getName()).log(Level.SEVERE, null, ex);
            throw new IOException("haha");
        }
        long d = 45378534268293459L;
        out.writeLong(d);
        if (in.readLong() != d) {
            throw new IOException("bad magic");
        }
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
                            console.log(this + " TOOK MORE THAN " + kademliaRef.settings.pingTimeoutSec + " SECONDS TO RESPOND TO " + r + ". CLOSING CONNECTION.");
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
            pendingRequests.remove(r.requestID);
            r.onError(this);
            //technically I'm pretty sure that an IOException here means that the entire connection is closed...
            //TODO
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
        for (long requestid : new ArrayList<>(pendingRequests.keySet())) {
            //if this weren't here we would have hella memory leaks
            //because some of the requests contain references to lookup objects / storeddata objects
            Request r = pendingRequests.remove(requestid);//crucial to remove as we go through the list, instead of getting and clearing at the end
            if (r == null) {
                throw new ConcurrentModificationException("god damn you");
            }
            r.onError(this);
        }
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
