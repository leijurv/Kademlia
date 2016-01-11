package kademlia;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import kademlia.lookup.Lookup;

/**
 *
 * @author leijurv
 */
public class ECPoint {
    public static final ECPoint base = new ECPoint(new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".toLowerCase(), 16), new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8".toLowerCase(), 16));
    public static final BigInteger modulus = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F".toLowerCase(), 16);
    private static final BigInteger three = new BigInteger("3");
    private static final BigInteger two = new BigInteger("2");
    private static final BigInteger seven = new BigInteger("7");
    private static final BigInteger negativeOne = new BigInteger("-1");
    private final BigInteger x;
    private final BigInteger y;
    public ECPoint(BigInteger x, BigInteger y) {
        this.x = x;
        this.y = y;
    }
    public ECPoint(DataInputStream input) throws IOException {
        byte[] pointX = new byte[33];
        byte[] pointY = new byte[33];
        input.readFully(pointX, 1, 32);
        input.readFully(pointY, 1, 32);
        this.x = new BigInteger(pointX);
        this.y = new BigInteger(pointY);
    }
    public ECPoint add(ECPoint q) {
        BigInteger s;
        if (q.x.equals(x)) {
            if (!q.y.equals(y)) {
                throw new IllegalStateException("kush");
            }
            BigInteger xs = x.multiply(x);
            s = xs.add(xs).add(xs).multiply(y.add(y).modInverse(modulus)).mod(modulus);
        } else {
            s = y.subtract(q.y).add(modulus).multiply(x.subtract(q.x).modInverse(modulus)).mod(modulus);
        }
        BigInteger xR = s.multiply(s).subtract(x).subtract(q.x).mod(modulus);
        BigInteger yR = modulus.subtract(y.add(s.multiply(xR.subtract(x))).mod(modulus));
        return new ECPoint(xR, yR);
    }
    public ECPoint multiply(BigInteger r) {
        if (r.getLowestSetBit() != 0) {
            return add(this).multiply(r.shiftRight(1));
        }
        if (r.equals(BigInteger.ONE)) {
            return this;
        }
        return add(add(this).multiply(r.shiftRight(1)));
    }
    public boolean verify() {
        return verify(x, y);
    }
    public static boolean verify(BigInteger x, BigInteger y) {
        return x.multiply(x).multiply(x).add(seven).mod(modulus).equals(y.multiply(y).mod(modulus)) && x.equals(x.mod(modulus)) && y.equals(y.mod(modulus));
    }
    @Override
    public String toString() {
        return x.toString(16) + "," + y.toString(16);
    }
    public static byte[] toNormal(byte[] input) {//Trim leading or add leading to make length 32.
        if (input.length == 32) {
            return input;
        }
        byte[] result = new byte[32];
        if (input.length > 32) {
            int off = input.length - 32;
            System.arraycopy(input, off, result, 0, 32);
            return result;
        }
        int off = 32 - input.length;
        System.arraycopy(input, 0, result, off, input.length);
        return result;
    }
    public void write(OutputStream out) throws IOException {
        out.write(toNormal(x.toByteArray()));
        out.write(toNormal(y.toByteArray()));
    }
    public long publicKeyHash() {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        try {
            write(o);
        } catch (IOException ex) {
            Logger.getLogger(ECPoint.class.getName()).log(Level.SEVERE, null, ex);
            throw new IllegalStateException(ex);
        }
        return Lookup.unmaskedHash(o.toByteArray());
    }
}
