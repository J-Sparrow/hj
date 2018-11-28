package com.gizwits.opensource.appkit.BleModule.bluetooth;

/**
 * 作者：Lin on 18/3/19 20:08
 * 邮箱：yuanwenlin2014@foxmail.com
 */
public class BleInterface {
    public interface MessageCallback {
        void onMessageReceive(String data);
        void onMessageFailed(String msg);
    }
}
