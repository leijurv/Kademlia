/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kademlia;

import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author aidan
 */
public class KeyValueData {
    private final long rawKey;
    private byte[] rawValue;
    private final SimpleStringProperty key;
    private final SimpleStringProperty value;
    public KeyValueData(long key, byte[] value) {
        this.rawKey = key;
        this.rawValue = value;
        this.key = new SimpleStringProperty(key + "");
        this.value = new SimpleStringProperty(new String(value));
    }
    public KeyValueData(String key, String value) {
        this.rawKey = Lookup.hash(key.getBytes());
        this.rawValue = value.getBytes();
        this.key = new SimpleStringProperty(key + "");
        this.value = new SimpleStringProperty(value);
    }
    public KeyValueData(String key, byte[] value) {
        this.rawKey = Lookup.hash(key.getBytes());
        this.rawValue = value;
        this.key = new SimpleStringProperty(key + "");
        this.value = new SimpleStringProperty(new String(value));
    }
    public String getKey() {
        return key.get();
    }
    public long getRawKey() {
        return rawKey;
    }
    public void setRawValue(byte[] rawValue) {
        this.rawValue = rawValue;
        this.value.set(new String(rawValue));
    }
    public byte[] getRawValue() {
        return rawValue;
    }
    public String getValue() {
        return value.get();
    }
    public SimpleStringProperty getValueProperty() {
        return value;
    }
    public void setValue(String newValue) {
        this.value.set(newValue);
        this.rawValue = newValue.getBytes();
    }
}
