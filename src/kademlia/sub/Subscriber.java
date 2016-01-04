/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.sub;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import kademlia.Kademlia;
import kademlia.StoredData;

/**
 *
 * @author leijurv
 */
public class Subscriber {
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final SubscriptionManager managerRef;
    private final Object outLock = new Object();
    private final long dateEstablished;
    public Subscriber(Socket s, SubscriptionManager manager) throws IOException {
        this.socket = s;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.managerRef = manager;
        this.dateEstablished = System.currentTimeMillis();
    }
    public void onUpdate(long key, StoredData data, boolean wantFull) {
        try {
            onUpdate0(key, data, wantFull);
        } catch (IOException ex) {
            Logger.getLogger(Subscriber.class.getName()).log(Level.SEVERE, null, ex);
            onError();
        }
    }
    private void onUpdate0(long key, StoredData data, boolean wantFull) throws IOException {
        if (data != null && key != data.key) {
            throw new IllegalArgumentException("wtf");
        }
        byte[] cache = (data != null && wantFull) ? data.getData() : null;
        synchronized (outLock) {//todo replace with cached bytearrayoutputstream and wrapped dataoutputstream just like how its done in connection.java
            out.writeLong(key);
            out.writeBoolean(data == null);
            if (data != null) {
                data.write(out);//haha, terrible misuse of the function originally intended for saving metadata
                //todo ^^ fix duplicate sending of key (StoredData.write also writes the key)
                if (wantFull) {
                    out.write(cache);
                }
            }
        }
    }
    public void onError() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(Subscriber.class.getName()).log(Level.SEVERE, null, ex);
        }
        managerRef.onSubscriberError(this);
    }
    public void startExecutionThread() {
        Kademlia.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    execute();
                } catch (IOException ex) {
                    Logger.getLogger(Subscriber.class.getName()).log(Level.SEVERE, null, ex);
                    onError();
                }
            }
        });
    }
    private void execute() throws IOException {
        while (true) {
            readMessage();
        }
    }
    private void readMessage() throws IOException {
        byte mode = in.readByte();
        long key = in.readLong();
        switch (mode) {
            case 0://subscribe
                boolean wantCurrent = in.readBoolean();
                boolean wantFull = in.readBoolean();
                Kademlia.threadPool.execute(new Runnable() {//why start a new thread here? idk
                    @Override
                    public void run() {
                        try {
                            managerRef.onSubscriptionRequest(Subscriber.this, key, wantCurrent, wantFull);
                        } catch (IllegalStateException ex) {
                            Logger.getLogger(Subscriber.class.getName()).log(Level.SEVERE, null, ex);
                            onError();
                        }
                    }
                });
                break;
            case 1://unsubscribe
                managerRef.onUnsubscribeRequest(Subscriber.this, key);
                break;
            default:
                throw new IOException("Invalid mode: " + mode);
        }
    }
}
