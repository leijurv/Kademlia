/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia.sub;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import kademlia.Kademlia;
import kademlia.StoredData;
import kademlia.console;

/**
 *
 * @author leijurv
 */
public class SubscriptionManager {
    private final Object lock = new Object();
    private final ArrayList<Subscriber> subscribers;
    private final HashMap<Long, ArrayList<Subscription>> subscriptions;
    private final Kademlia kademliaRef;
    public SubscriptionManager(Kademlia kademliaRef) {
        subscribers = new ArrayList<>();
        subscriptions = new HashMap<>();
        this.kademliaRef = kademliaRef;
    }
    public void onSubscriberSocket(Socket s) throws IOException {
        Subscriber subscriber = new Subscriber(s, this);
        synchronized (lock) {
            subscribers.add(subscriber);
            subscriber.startExecutionThread();//do this in the lock because we dont want something else running on it before it has started listening
        }
    }
    public void onSubscriberError(Subscriber sub) {
        synchronized (lock) {
            subscribers.remove(sub);
            for (long key : subscriptions.keySet()) {
                onUnsubscribeRequest(sub, key);//lol code re-use
            }
        }
    }
    public void onUnsubscribeRequest(Subscriber sub, long key) {
        ArrayList<Subscription> subs = subscriptions.get(key);
        for (int i = 0; i < subs.size(); i++) {//dont use enhanced for loop because we might be modifying this array
            Subscription subscription = subs.get(i);
            if (subscription.hasSubscriber(sub)) {
                console.log("removing subscription " + subscription + " to key " + subscription.key + " because of error/UnsubscribeRequest with the associated subscriber " + sub);
                subs.remove(i);
                i--;//ignore this netbeans error, its not a typo, trust me
            }
        }
    }
    public void onSubscriptionRequest(Subscriber sub, long key, boolean wantCurrent, boolean wantFull) {
        Subscription subscription = new Subscription(sub, key, wantFull);
        synchronized (lock) {
            if (subscriptions.get(key) == null) {
                subscriptions.put(key, new ArrayList<>());
            }
            for (Subscription s : subscriptions.get(key)) {
                if (s.key == key) {
                    throw new IllegalStateException("dupe");
                }
            }
            subscriptions.get(key).add(subscription);
        }
        if (wantCurrent) {
            subscription.onUpdate(kademliaRef.storedData.getMetadata(key));
        }
    }
    public void onUpdateToKey(StoredData data) {
        synchronized (lock) {
            ArrayList<Subscription> subs = subscriptions.get(data.key);
            if (subs == null || subs.isEmpty()) {
                return;
            }
            console.log("telling all " + subs.size() + " subscriber(s) about the update to key " + data.key);
            for (Subscription sub : subs) {
                Kademlia.threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        sub.onUpdate(data);
                    }
                });
            }
        }
    }
}
