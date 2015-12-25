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
    public static DDT getFromMask(long k) {
        byte mask = (byte) (k & 0xff);
        for (DDT ddt : DDT.values()) {
            if (ddt.mask == mask) {
                return ddt;
            }
        }
        throw new IllegalStateException("bad mask");
    }
    public long mask(long k) {
        return (k & ~0xff) | mask;
    }
}
