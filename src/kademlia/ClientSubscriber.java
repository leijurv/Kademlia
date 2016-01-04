/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author leijurv
 */
public class ClientSubscriber {
    private final Node node;
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    public final Object outLock = new Object();
    public final Object subListLock = new Object();
    private final ArrayList<ClientSubscription> clientSubscriptions;
    public ClientSubscriber(Node node) throws IOException {
        clientSubscriptions = new ArrayList<>();
        this.node = node;
        socket = new Socket(node.host, node.port);
        in = new DataInputStream(socket.getInputStream());
        out = new DataOutputStream(socket.getOutputStream());
        out.write(7);//trust me
        startReadThread();
    }
    private void startReadThread() {
        Kademlia.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doRead();
                } catch (IllegalStateException | IOException ex) {
                    Logger.getLogger(ClientSubscriber.class.getName()).log(Level.SEVERE, null, ex);
                    onError();
                }
            }
        });
    }
    private void doRead() throws IOException {
        while (true) {
            readMessage();
        }
    }
    private void readMessage() throws IOException {
        long key = in.readLong();
        ClientSubscription theClientSub = null;
        synchronized (subListLock) {
            for (ClientSubscription clientSub : clientSubscriptions) {
                if (clientSub.key == key) {
                    if (theClientSub != null) {
                        throw new IllegalStateException(clientSub + " and " + theClientSub + " are both subbed to key " + key);
                    }
                    theClientSub = clientSub;
                }
            }
            if (theClientSub == null) {
                throw new IllegalStateException("I didn't ask for this " + key);
            }
        }
        theClientSub.onSubData();//netbeans is awesome because it realizes that I couldn't possibly be dereferencing a null pointer here
        //suck it eclipse
    }
    private void onError() {
        try {
            socket.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientSubscriber.class.getName()).log(Level.SEVERE, null, ex);
        }
        synchronized (subListLock) {
            for (ClientSubscription clientSub : clientSubscriptions) {
                clientSub.onError();
            }
        }
    }
    public ClientSubscription subscribeTo(long key) throws IOException {
        synchronized (subListLock) {
            for (ClientSubscription clientSub : clientSubscriptions) {
                if (clientSub.key == key) {
                    throw new IllegalArgumentException("Already subscribed to that m9");
                }
            }
        }
        ClientSubscription client = new ClientSubscription(node, socket, in, out, key, this);
        synchronized (subListLock) {
            clientSubscriptions.add(client);
        }
        client.sendSubInfo();//send this AFTER adding to list, because otherwise we could get a response before it was added to the list
        return client;
    }
}
