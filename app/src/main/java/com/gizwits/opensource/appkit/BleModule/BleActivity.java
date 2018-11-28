package com.gizwits.opensource.appkit.BleModule;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.opensource.appkit.BleModule.bluetooth.CHCarBleClient;
import com.gizwits.opensource.appkit.BleModule.entity.BleDeviceEntity;
import com.gizwits.opensource.appkit.GosApplication;
import com.gizwits.opensource.appkit.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressLint("SetTextI18n")
public class BleActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int SEARCHACTIVITY_CONECT = 2;
    private TextView mBtConnectState;
    private BluetoothAdapter mBluetoothAdapter;
    private List<BleDeviceEntity> list = new ArrayList<>();
    private List<BleDeviceEntity> bluetoothList = new ArrayList<>();
    private final int CONNECTED = 0;
    private final int DISCONNECTED = 1;
    public String bleName = "";
    public static String bleNameConected = "";
    private ProgressDialog progressDialog;

    //    AlertDialog alertDialog = new AlertDialog.Builder(this).create();
    Long startTime = 0L;
    boolean isSearching = false;
    MyHandler mHandler = new MyHandler();
    private SensorManager sensorManager;  //定义传感器管理器
    private Vibrator vibrator;            //定义振动器

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //全屏显示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        hideBottomUIMenu();
        setContentView(R.layout.activity_ble);
        Log.d("BluetoothLeService", "ble,onCreat");
        initView();
        initBluetooth();
        Log.d("BluetoothLeService", "ble,onCreated");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("BluetoothLeService", "ble,Resume");

        scanLeDevice(true);
//        //为加速度传感器注册监听器
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_GAME);
        Log.d("ble", "Ble,Resume");
    }

    private void initView() {
        findViewById(R.id.home_ble_search).setOnClickListener(this);
        findViewById(R.id.home_ble_close).setOnClickListener(this);
        mBtConnectState = (TextView) findViewById(R.id.home_ble_state);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);  // 获取传感器管理器
        vibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);  //获取振动器服务
    }

    private void initBluetooth() {
        //申请定位权限，才能用蓝牙
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
            }
        }
//        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        if (mBluetoothAdapter == null) {//设备不支持蓝牙
//            Toast.makeText(getApplicationContext(), "设备不支持蓝牙",
//                    Toast.LENGTH_LONG).show();
//            finish();
//            return;
//        }
        //判断蓝牙是否开启
//        if (!mBluetoothAdapter.isEnabled()) {//蓝牙未开启
//            Intent enableIntent = new Intent(
//                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
//            //mBluetoothAdapter.enable();此方法直接开启蓝牙，不建议这样用。
//        }

        //注册广播接收者，监听扫描到的蓝牙设备

//        IntentFilter filter = new IntentFilter();
//        //发现设备
//        filter.addAction(BluetoothDevice.ACTION_FOUND);
//        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
//        registerReceiver(mBluetoothReceiver, filter);
//
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        //判断蓝牙是否开启
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, REQUEST_ENABLE_BT);
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();

//        mBluetoothAdapter.startDiscovery();
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        float[] values = sensorEvent.values;  //获取传感器X、Y、Z三个轴的输出信息
        int sensorType = sensorEvent.sensor.getType();  // 获取传感器类型
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {  //如果是加速度传感器
            //X轴输出信息>15,Y轴输出信息>15,Z轴输出信息>20
            if ((values[0] > 15 || values[1] > 15 || values[2] > 20) && !isSearching) {
                Log.d("BluetoothLeService", "摇一摇");
                scanLeDevice(true);//开始搜索
                isSearching = true;
                if (!GosApplication.getmBleClient().isConnected()) {
                    //todo ????????????为什么有时候会出空指针，找不到activity
                    if (!isFinishing()) {
                        startTime = System.currentTimeMillis();
                        progressDialog = ProgressDialog.show(BleActivity.this,
                                getResources().getString(R.string.searching),
                                getResources().getString(R.string.please_wait_a_moment),
                                true);//显示加载框
                        handler.post(runnable);
                    } else {
                        Log.d("BluetoothLeService", "ble is finished");
                    }
                } else {
                    Toast.makeText(this,"蓝牙已经连接",Toast.LENGTH_LONG);
                    isSearching = false;
                }
            }
        }
    }

    ////******************************震动传感器触发事件************************/////
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            // TODO Auto-generated method stub
            //如果列表中没有蓝牙
            if (bluetoothList.size() == 0) {
                Long currentTime = System.currentTimeMillis();
                if (currentTime - startTime > 5000) {
                    progressDialog.dismiss();
                    progressDialog = ProgressDialog.show(BleActivity.this,  getResources().getString(R.string.bluetooth_has_not_been_found),  getResources().getString(R.string.please_try_again), true);//显示加载框
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    }, 1000);
//                    mHandler.sendEmptyMessage(DISCONNECTED);
                    isSearching = false;
                    handler.removeCallbacks(runnable);
                    scanLeDevice(false);
                }else {
                    handler.postDelayed(this, 200);
                }
            } else {
                //如果列表中有蓝牙
                scanLeDevice(false);
                final BleDeviceEntity ble = bluetoothList.get(0);
                BluetoothDevice device = ble.getDevice();
                try {
                    GosApplication.getmBleClient().connect(device.getAddress());

                    //如果5秒后还未连接，则断开连接，并提示重连
//                    conenecting = true;
//                    mHandler.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if(conenecting){
//                                progressDialog.dismiss();

//                    Toast.makeText(BleActivity.this, "蓝牙连接失败，请重试", Toast.LENGTH_SHORT).show();


//                                progressDialog = ProgressDialog.show(BleActivity.this, "蓝牙连接失败，请重试", "尝试连接 " + ble.getName() + "失败", true);//显示加载框
//                                new Handler().postDelayed(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        progressDialog.dismiss();
//                                    }
//                                }, 1000);


//                                AppContext.getmBleClient().disConnect();
//                                conenecting = false;
//                            }
//                        }
//                    }, 12000L);
                    progressDialog.dismiss();
                    progressDialog = ProgressDialog.show(BleActivity.this,  getResources().getString(R.string.connecting),  getResources().getString(R.string.connecting_to)+ ble.getName(), true);//显示加载框
                    GosApplication.getmBleClient().setmConnectRequest(new CHCarBleClient.ConnectionRequest() {
                        @Override
                        public void connectSuccess() {
                            bleNameConected = ble.getName();
                            mHandler.sendEmptyMessage(CONNECTED);
                        }

                        @Override
                        public void connectFailed() {
                            mHandler.sendEmptyMessage(DISCONNECTED);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
                vibrator.vibrate(200);                    //设置振动器频率
                sensorManager.unregisterListener(BleActivity.this);  //取消注册监听器
                isSearching = false;
                handler.removeCallbacks(runnable);
            }
        }
    };

    //****************************新的蓝牙搜索方法**************************
    boolean mScanning = false;
    long SCAN_PERIOD = 10000;

    private void scanLeDevice(final boolean enable) {

//todo  安卓5,0之后建议用这个函数，不知道稳定不稳定，还没试
//        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
//        scanner.startScan(leCallback);
//          scanner.stopScan(leCallback);

        if (enable) {
//             Stops scanning after a pre-defined scan period.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
//
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback); //开始搜索
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);//停止搜索
        }
    }
    // 搜索的操作最好放在Activity的onResume里面或者服务里面，我有发现放在onCreate有时响应不及时

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            //todo 换到新线程中执行？
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if (device == null || device.getName() == null) return;
                    bluetoothListAdd(device, rssi);
                }
            }).start();

//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    //在这里可以把搜索到的设备保存起来
//                    //device.getName();获取蓝牙设备名字
//                    //device.getAddress();获取蓝牙设备mac地址
//                    if (device == null || device.getName() == null) return;
//                    bluetoothListAdd(device, rssi);
//                }
//            });
        }
    };

    //****************************搜索到新的蓝牙时添加到List中********************************
    private void bluetoothListAdd(BluetoothDevice device, int rssi) {
        String ibeaconName = "";
        if (null == device.getName()) {
            ibeaconName = "unknown_bluetooth";
        } else {
            ibeaconName = device.getName();
        }
        String mac = device.getAddress();
        if (ibeaconName.length() >= 4) {
            boolean flag = true;
            for (int x = 0; x < list.size(); x++) {
                if (mac.equals(list.get(x).getMac())) {
                    flag = false;
                }
            }
            if (flag) {
                if ("Find".equals(ibeaconName.substring(0, 4))) {
                    list.add(new BleDeviceEntity(ibeaconName, mac, rssi, device));
                    Log.d("麦克mac",mac);
                    handler.sendEmptyMessage(111);
                }
            }
        }
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 111) {
                bluetoothList.clear();
                bluetoothList.addAll(list);
                Collections.sort(bluetoothList);
            }
            return true;
        }
    });
    //***********************************************************************************************//

//    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            String action = intent.getAction();
//            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
//                //获取蓝牙设备
//                BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                if (scanDevice == null || scanDevice.getName() == null) return;
////                Log.d(TAG, "name="+scanDevice.getName()+"address="+scanDevice.getAddress());
//                //蓝牙设备名称
//                int rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
//
//                String ibeaconName = "";
//                if (null == scanDevice.getName()) {
//                    ibeaconName = "unknown_bluetooth";
//                } else {
//                    ibeaconName = scanDevice.getName();
//                }
//                String mac = scanDevice.getAddress();
//                boolean flag = true;
//                for (int x = 0; x < list.size(); x++) {
//                    if (mac.equals(list.get(x).getMac())) {
//                        flag = false;
//                    }
//                }
//                if (flag) {
//                    if (ibeaconName.length() >= 4) {
//                        if ("Find".equals(ibeaconName.substring(0, 4))) {
//                            list.add(new BleDeviceEntity(ibeaconName, mac, rssi, scanDevice));
//                            handler.sendEmptyMessage(111);
//                        }
//                    }
//                }
//            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
//            }
//        }
//    };

    //todo  dialog是否会引起内存泄漏
//    static class DiaHandler extends Handler{
//
//        }

    //是否连接成功的消息处理
    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECTED:
                    progressDialog.dismiss();

                        //

                        if (!isFinishing()) {
                            progressDialog = ProgressDialog.show(BleActivity.this, getResources().getString(R.string.Bluetooth_is_connected_successful), getResources().getString(R.string.Bluetooth_is_connected_to) + bleNameConected, true);//显示加载框
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.dismiss();
                                    finish();
                                }
                            }, 500);
                        } else {
                            Log.d("BluetoothLeService", "ble is finished");
                        }

                    break;
                case DISCONNECTED:
//                    //为加速度传感器注册监听器
//                    sensorManager.registerListener(BleActivity.this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                            SensorManager.SENSOR_DELAY_GAME);
                    progressDialog.dismiss();
                    if ("BleActivity".equals(getClass().getSimpleName())) {
                        progressDialog = ProgressDialog.show(BleActivity.this,
                                getResources().getString(R.string.Bluetooth_connection_failure_please_try_again),
                                "" ,
                                true);//显示加载框

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        }, 1000);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.home_ble_search:
                Intent intent = new Intent(BleActivity.this, SearchActivity.class);
                startActivityForResult(intent, SEARCHACTIVITY_CONECT);
                break;
            case R.id.home_ble_close:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case SEARCHACTIVITY_CONECT:
                if (resultCode == RESULT_OK) {
                    //TODO 显示连接结果
//                    Toast.makeText(BleActivity.this,"连接成功",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case REQUEST_ENABLE_BT:
//                if (resultCode == REQUEST_ENABLE_BT) {
////                    Toast.makeText(this, "蓝牙已启用", Toast.LENGTH_SHORT).show();
//                } else {
////                    Toast.makeText(this, "蓝牙未启用", Toast.LENGTH_SHORT).show();
//                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //取消搜索
        scanLeDevice(false);
        sensorManager.unregisterListener(BleActivity.this);  //取消注册监听器
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mBluetoothAdapter.cancelDiscovery(); //取消扫描
//        unregisterReceiver(mBluetoothReceiver);
        Log.d("ble", "Ble,Pause");
    }
}
