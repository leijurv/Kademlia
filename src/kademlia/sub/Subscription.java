/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.sub;

import kademlia.StoredData;

/**
 *
 * @author leijurv
 */
public class Subscription {
    private final Subscriber subscriber;
    final long key;
    private volatile int numUpdates;
    private final long dateEstablished;
    private volatile long lastUpdate;
    private final boolean wantFull;
    public Subscription(Subscriber subscriber, long key, boolean wantFull) {
        this.subscriber = subscriber;
        this.key = key;
        this.numUpdates = 0;
        this.wantFull = wantFull;
        this.dateEstablished = System.currentTimeMillis();
    }
    public void onUpdate(StoredData data) {
        if (data != null && data.key != key) {
            throw new IllegalArgumentException("this is a subscription to " + key + ", not " + data.key);
        }
        numUpdates++;
        lastUpdate = System.currentTimeMillis();
        subscriber.onUpdate(key, data, wantFull);
    }
    public boolean hasSubscriber(Subscriber sub) {
        return subscriber.equals(sub);
    }
}
