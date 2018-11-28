package com.gizwits.opensource.appkit;

import android.app.Application;
import android.util.Log;

import com.gizwits.opensource.appkit.BleModule.bluetooth.CHCarBleClient;

public class GosApplication extends Application {
    private final static String TAG = GosApplication.class.getSimpleName();

    private static CHCarBleClient mBleClient;
    private static GosApplication instance;
    public static GosApplication getInstance(){
        return instance;
    }
//    private String receiveData = "";
//    public  String find_line_Result;
//    public  String ultrasonic_Result;
//    public boolean ifget666 = false;             //是否接收到666

    @Override
    public void onCreate(){
        super.onCreate();
        instance = this;
        Log.d("好","init");
        initBle();
    }

    public void initBle(){
        mBleClient = new CHCarBleClient(this);
    }

    public static CHCarBleClient getmBleClient() {
        return mBleClient;
    }

    @Override
    public void onTerminate() {
        // 程序终止的时候执行
        mBleClient.onDescory();
        super.onTerminate();
    }
}
