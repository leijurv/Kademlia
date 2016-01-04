/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

/**
 *
 * @author leijurv
 */
public enum DDT {
    STANDARD_PUT_GET((byte) 0), FILE_METADATA((byte) 7), CHUNK((byte) 42);
    public final byte mask;
    DDT(byte mask) {
        this.mask = mask;
    }
    public static DDT getFromKey(long key) {
        byte mask = (byte) (key & 0xff);
        for (DDT ddt : DDT.values()) {
            if (ddt.mask == mask) {
                return ddt;
            }
        }
        throw new IllegalStateException("bad mask " + mask + " for key " + key);
    }
    public long mask(long k) {
        return (k & ~0xff) | mask;
    }
}
