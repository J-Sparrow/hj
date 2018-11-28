package com.gizwits.opensource.appkit.BleModule.entity;

import android.bluetooth.BluetoothDevice;
import android.support.annotation.NonNull;

/**
 * Created by fengweilun on 2017/10/25.
 */

public class BleDeviceEntity implements Comparable<BleDeviceEntity> {

    private String name;
    private String mac;
    private int rssi;
    private BluetoothDevice device ;

    public BleDeviceEntity() {
    }

    public BleDeviceEntity(String name, String mac, int rssi, BluetoothDevice device) {
        this.name = name;
        this.mac = mac;
        this.rssi = rssi;
        this.device = device;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public int compareTo(@NonNull BleDeviceEntity bleDeviceEntity) {
        int i = bleDeviceEntity.getRssi()-this.getRssi();//先按照年龄排序
        return i;
    }
}
