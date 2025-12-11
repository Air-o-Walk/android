package com.example.airowalkmenu.data.models;

public class Medicion {

    private final String mac;
    private final int rssi;
    private final int txPower;
    private final int major;
    private final int minor;
    private final int sensor;
    private final float valor;
    private final long timestamp;

    public Medicion(String mac, int rssi, int txPower, int major, int minor,
                    int sensor, float valor, long timestamp) {

        this.mac = mac;
        this.rssi = rssi;
        this.txPower = txPower;
        this.major = major;
        this.minor = minor;
        this.sensor = sensor;
        this.valor = valor;
        this.timestamp = timestamp;
    }

    public String getMac() { return mac; }
    public int getRssi() { return rssi; }
    public int getTxPower() { return txPower; }
    public int getMajor() { return major; }
    public int getMinor() { return minor; }
    public int getSensor() { return sensor; }
    public float getValor() { return valor; }
    public long getTimestamp() { return timestamp; }
}