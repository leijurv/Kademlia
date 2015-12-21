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
    private final byte[] rawValue;
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
    public String getKey() {
        return key.get();
    }
    public String getValue() {
        return value.get();
    }
    public void setValue(String newValue) {
        value.set(newValue);
    }
}
