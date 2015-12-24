/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 *
 * @author leijurv
 */
public class RequestTest extends Request {
    private final StoredData storedData;
    private final long key;
    public RequestTest(StoredData storedData) {
        super((byte) 4);
        this.storedData = storedData;
        this.key = storedData.key;
    }
    protected RequestTest(long requestID, DataInputStream in) throws IOException {
        super(requestID, (byte) 4);
        this.storedData = null;
        this.key = in.readLong();
    }
    @Override
    public void sendData(DataOutputStream out) throws IOException {
        out.writeLong(key);
    }
    @Override
    public void execute(Kademlia kademliaRef, DataOutputStream out) throws IOException {
        StoredData data = kademliaRef.storedData.getMetadata(key);
        out.writeBoolean(data != null);
        if (data != null) {
            out.writeLong(data.hash);
            out.writeLong(data.lastModified);
            out.writeLong(data.size);
        }
    }
    @Override
    public void onResponse(DataInputStream in, Connection conn) throws IOException {
        boolean has = in.readBoolean();
        if (has) {
            long hash = in.readLong();
            long lastModified = in.readLong();
            long size = in.readLong();
            boolean sameHash = hash == storedData.hash;
            boolean sameSize = size == storedData.size;
            boolean sameLM = lastModified == storedData.lastModified;
            if (sameHash && sameSize && sameLM) {
                console.log("we good for " + conn + " and " + key);
            } else {
                boolean areWeNewer = storedData.lastModified > lastModified;
                console.log("we not good for " + conn + " and " + key + ": " + sameHash + " " + sameSize + " " + sameLM + " " + areWeNewer);
                if (sameLM) {
                    console.log("weird. same date for last modified, but different hash and/or size... did two people modify this file at the exact same millisecond?");
                    storedData.storeCopyIn(conn);//<--- so here's my reasoning for this
                    //at this point we know that there are two competing versions on the network
                    //with no standardizable way of determining which is more popular/prominent, because the lastModified is identical for the two versions
                    //if we don't copy here, then of the k nodes with this thing, some static amount of them will have one version and the rest will have another
                    //and it will remain a standoff like that
                    //however, if we DO copy here, then it will be in a constant flux, and I think that most likely the k nodes will eventually settle on one
                    //but only if they are constantly overwriting each other in this way with the new version
                    //note: this will work because nodes will accept an overwrite with a last modified that is greater OR EQUAL TO their current stored last modified
                    return;
                }
                if (areWeNewer) {
                    console.log("we have a newer version of " + key + ", sending to " + conn);
                    storedData.storeCopyIn(conn);
                    //if we are newer then they will happily acccept this overwrite and pass it on to the others
                }
                //so at this point we know we have an older version.
                //TODO ask them here for the new version and store it
                //low priority because currently this will happen eventually when they send a corresponding requesttest to US and we respond with that we have an older version, which will cause them to send us the new version
            }
        } else {
            console.log("Sending " + key + " on connection " + conn + " because they don't have it and they should");
            storedData.storeCopyIn(conn);//they don't have it, so send it to them
        }
    }
    @Override
    public void onError(Connection conn) {
    }
}
