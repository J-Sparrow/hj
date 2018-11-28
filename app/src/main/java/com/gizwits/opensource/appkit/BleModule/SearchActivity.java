package com.gizwits.opensource.appkit.BleModule;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gizwits.opensource.appkit.BleModule.bluetooth.CHCarBleClient;
import com.gizwits.opensource.appkit.BleModule.entity.BleDeviceEntity;
import com.gizwits.opensource.appkit.GosApplication;
import com.gizwits.opensource.appkit.R;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;


public class SearchActivity extends Activity {

    private static final String TAG = "SearchActivity";
    private BluetoothAdapter mBluetoothAdapter;
    private List<BleDeviceEntity> list = new ArrayList<>();
    private List<BleDeviceEntity> bluetoothList = new ArrayList<>();
    Timer timer;

    private BleAdapter bleAdapter;
    private ListView listView;
    private ImageView button;

    private ProgressDialog progressDialog;
    private ProgressDialog progressDialogChangeName;

    private final int CONNECTED = 0;
    private final int DISCONNECTED = 1;

    MyHandler myHandler = new MyHandler();

    class MyHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case CONNECTED:
                    progressDialog.dismiss();


                      finish();

                    break;
                case DISCONNECTED:
                    Log.d(TAG,"连接断开");
                    Toast.makeText(SearchActivity.this, "连接失败", Toast.LENGTH_LONG).show();
                    progressDialog.dismiss();
                    finish();
                    break;
                default:
                    break;
            }
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initView();
//        AppContext.getmBleClient().disConnect();
//        timer = new Timer();
//        timer.schedule(new TimerTask1(),3000,2000);//tiemr.schedule(执行的方法,延迟时间,多久执行一次)

    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 111) {
                bluetoothList.clear();
                bluetoothList.addAll(list);
                Collections.sort(bluetoothList);
                bleAdapter.notifyDataSetChanged();
            }
            return true;
        }
    });
//
//    //每隔两秒清空一次列表
//    class TimerTask1 extends TimerTask {
//        @Override
//        public void run() {
//            list.clear();
//
//        }
//    }

    private void initView() {
        button = (ImageView) findViewById(R.id.bluetooth_close);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        initBluetooth();
        listView = (ListView) findViewById(R.id.search_listview);
        bleAdapter = new BleAdapter(this, R.layout.item_search, bluetoothList);
        listView.setAdapter(bleAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                final BleDeviceEntity ble = bluetoothList.get(i);
                BluetoothDevice device = ble.getDevice();
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setText(device.getAddress());
//                Toast.makeText(getApplicationContext(), R.string.connecting_please_wait_a_moment, Toast.LENGTH_LONG);
                try {
                  Boolean isSucess =  GosApplication.getmBleClient().connect(device.getAddress());
                  Log.d("麦克mac",device.getAddress());
                  if(!isSucess){
                      setResult(RESULT_CANCELED);
                      myHandler.sendEmptyMessage(DISCONNECTED);
                      Log.d("","连接动作失败，1");
                  }
                    progressDialog = ProgressDialog.show(SearchActivity.this,
                           "正在连接………………",
                            "连接中",
                            true);//显示加载框
                    bluetoothList.clear();
                    GosApplication.getmBleClient().setmConnectRequest(new CHCarBleClient.ConnectionRequest() {
                        @Override
                        public void connectSuccess() {
                            setResult(RESULT_OK);
                            Message msg = new Message();
                            msg.what = CONNECTED;
                            msg.obj = ble.getName();
                            myHandler.sendMessage(msg);
//                                progressDialog.dismiss();
                        }

                        @Override
                        public void connectFailed() {
                            setResult(RESULT_CANCELED);
                            myHandler.sendEmptyMessage(DISCONNECTED);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initBluetooth() {

        //注册广播接收者，监听扫描到的蓝牙设备
        IntentFilter filter = new IntentFilter();
        //发现设备
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mBluetoothReceiver, filter);

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
        }
        mBluetoothAdapter.startDiscovery();
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //获取蓝牙设备
                BluetoothDevice scanDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (scanDevice == null || scanDevice.getName() == null) return;
//                Log.d(TAG, "name="+scanDevice.getName()+"address="+scanDevice.getAddress());
                //蓝牙设备名称
                int rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);

                String ibeaconName = "";
                if (null == scanDevice.getName()) {
                    ibeaconName = "unknown_bluetooth";
                } else {
                    ibeaconName = scanDevice.getName();
                }
                String mac = scanDevice.getAddress();
                boolean flag = true;
                for (int x = 0; x < list.size(); x++) {
                    if (mac.equals(list.get(x).getMac())) {
                        flag = false;
                    }
                }
                if (flag) {
                    if (ibeaconName.length() >= 4) {
                        if ("Find".equals(ibeaconName.substring(0, 4))) {
                            list.add(new BleDeviceEntity(ibeaconName, mac, rssi, scanDevice));
                            Log.d("麦克mac",mac);
                            handler.sendEmptyMessage(111);
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter.cancelDiscovery(); //取消扫描
        unregisterReceiver(mBluetoothReceiver);
        Log.d("ble", "Search,Destroy");
    }
}