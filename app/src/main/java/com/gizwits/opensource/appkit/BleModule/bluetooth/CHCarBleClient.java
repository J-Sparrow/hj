package com.gizwits.opensource.appkit.BleModule.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import com.gizwits.opensource.appkit.BleModule.Util.LogUtils;
import com.gizwits.opensource.appkit.BleModule.Util.DeviceInstructionBuilder;
import com.gizwits.opensource.appkit.BleModule.Util.UnicodeUtil;
import com.gizwits.opensource.appkit.BleModule.entity.BleMessageEntity;

import java.util.ArrayList;
import java.util.UUID;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * 作者：Lin on 18/3/9 17:02
 * 邮箱：yuanwenlin2014@foxmail.com
 */
public class CHCarBleClient {
    private final static String TAG = CHCarBleClient.class.getSimpleName();

    private final String ERROR_MSG = "指令超时未回复";

    public static final byte[] TESTBYTES = new byte[]{(byte) 0xbb, (byte) 0x66, (byte) 0x07, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0xFF, (byte) 0x00};

    private static final long TIMEOUT_DEFAULT = 500;//默认的单条指令超时记录
    private static final boolean TIMEOUT_RESEND_DEFULT = true;//默认的超时重发机制
    private int repushTimes = 0;//记录当前指令发送的次数
    private static final int REPUSH_MAX = 3;//最多发3次
    private static final int INSTRUCTION_MAX = 10;//最多发3次

    private int keyCode = 0;//当前指令的回执编码

    //需要发送的消息列表
    private ArrayList<BleMessageEntity> msgArrayList = new ArrayList<>();

    //只用于超时操作
    private Handler mHandler = new Handler();
    /**
     * 当前连接设备 连接状态
     */
    private BluetoothDevice mDevice;
    private boolean connected = false;
    private BleMessageEntity currentMsg;

    private long currentTime = System.currentTimeMillis();

    /**
     * 我们通信所需要的蓝牙gattCharacter
     */
    private BluetoothGattCharacteristic mSendCharacter, mReadCharacter;

    private static Context mContext;
    //蓝牙服务
    private BluetoothLeService mBluetoothLeService;

    private CHCarBleClient() {
    }

    public CHCarBleClient(Context context) {
        mContext = context;
        init(context);
    }

    private ConnectionRequest mConnectRequest;

    /**
     * 初始化
     * 注册mGattUpdateReceiver
     *
     * @param context 上下文
     */
    private void init(Context context) {
        mContext = context;
        mContext.registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());//注册广播
        Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        Log.d("好","intclient");
        mContext.bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);//绑定service
    }


    public void setmConnectRequest(ConnectionRequest mConnectRequest) {
        this.mConnectRequest = mConnectRequest;
    }

    /**
     * 使用本类结束时记得调用
     * 广播需要
     */
    public void onDescory() {
        mContext.unregisterReceiver(mGattUpdateReceiver);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.d("好 serve  ","" + mBluetoothLeService);
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                mDevice = mBluetoothLeService.getMsgBluetoothGatt().getDevice();

                //已连接
                if (mConnectRequest != null) {
                    LogUtils.d(TAG, "蓝牙连接基础已建立");
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                mDevice = null;
                LogUtils.d(TAG, "收到蓝牙断开的广播");

                if (mConnectRequest != null) {
                    mConnectRequest.connectFailed();
                }
                //已断开
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                Log.d(TAG, "发现service");
                //发现service
                mHandler.postDelayed(initBleService, 1000);
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                //收到消息
                byte[] bs = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                String byteString = UnicodeUtil.bytes2HexStr(bs);
                receiveMsg(byteString);
            }
        }
    };


    public boolean connect(String address) {
//        address = "20:C3:8F:E3:38:5D";
        Log.d("好","" + mBluetoothLeService);
       return mBluetoothLeService.connect(address);
    }

    public void disConnect() {
        mBluetoothLeService.disconnect();
    }

    public void closeGatt(){
        mBluetoothLeService.close();
    }

    private void sendMsg(byte[] msg) {
        currentTime = System.currentTimeMillis();
        Log.d(TAG, "准备发送" + msgArrayList.size());
        if (mBluetoothLeService != null) {
            mBluetoothLeService.write(msg, mSendCharacter);
        } else {
            Log.d(TAG, "蓝牙服务为空");
        }
    }


    private String msg = "";//暂存收到的消息
    /**
     * 收到蓝牙消息时的第一步处理,如果消息不完整则暂存,等待完整后再处理
     *
     * @param msg 收到的消息
     */
    private void receiveMsg(String msg) {

        Log.d(TAG, "收消息片段 :" + msg);
        if (msg.contains("BB 66")) {
            this.msg = "";
        }
        this.msg = this.msg + msg;
        if (this.msg.contains("0D 0A")) {
            receiveWholeMsg();
        } else {
            this.msg = this.msg + " ";
        }
    }

    /**
     * 收到完整消息后的处理
     */
    private void receiveWholeMsg() {

        if (msg.contains("BB 66 ") && msg.contains("0D 0A")) {

            //将msg以BB 66为开头 0D 0A为结尾进行裁剪
            msg = msg.split(" 0D 0A")[0];
            msg = msg.split("BB 66 ")[1];

            String keyCode = msg.substring(0, 2);
            if (currentMsg != null) {
                if (Integer.valueOf(keyCode, 16) == currentMsg.getKeyCode()) {//收到正确指令

                    mHandler.removeCallbacks(bleTimeoutRunnable);
                    String data = "";
                    if (msg.length() > 3)
                        data = msg.substring(3, msg.length());
                    if (currentMsg.getMsgRequestInterface() != null)
                        currentMsg.getMsgRequestInterface().onMessageReceive(data);

                    removeFirstMsg();

                    Log.d(TAG, "收消息完整:  " + msg +
                            "   Time:" + System.currentTimeMillis() +
                            "   耗时:" + (currentTime - System.currentTimeMillis()) +
                            "   剩余指令:" + msgArrayList.size());

                    msg = "";
                    if (!msgArrayList.isEmpty())
                        push();
                } else {//收到的指令回执错误时
                    Log.d(TAG, "回执错误:忽略指令 get:" + Integer.valueOf(keyCode, 16) + " Should Be:" + currentMsg.getKeyCode());
                }
            } else {//当前没有指令请求,小车还是发了消息回来导致的.
                Log.d(TAG, "当前指令为空 忽略");
            }
        } else {
            Log.d(TAG, "指令格式错误 忽略");
        }
    }

    /**
     * 获取所需要的广播列表
     *
     * @return 广播列表
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_WRITE_RESULT);
        return intentFilter;
    }

    Runnable initBleService = new Runnable() {
        @Override
        public void run() {
            initBleService();
        }
    };

    void initBleService() {
        //初始化发送和接受Character
        LogUtils.d(TAG,"开始查找服务");
        BluetoothGatt mGatt = mBluetoothLeService.getMsgBluetoothGatt();
        BluetoothGattService myServices = mGatt.getService(UUID.fromString(SampleGattAttributes.MESSAGE_GATT));

        if (myServices != null) {
            LogUtils.d(TAG,"开始查找服务1");
            mSendCharacter = myServices.getCharacteristic(UUID.fromString(SampleGattAttributes.MESSAGE_SEND));
            mReadCharacter = myServices.getCharacteristic(UUID.fromString(SampleGattAttributes.MESSAGE_READ));
            if (mSendCharacter != null && mReadCharacter != null) {
                LogUtils.d(TAG,"开始查找服务2");
                int proper = mReadCharacter.getProperties();
                if ((0 != (proper & BluetoothGattCharacteristic.PROPERTY_NOTIFY))
                        || (0 != (proper & BluetoothGattCharacteristic.PROPERTY_INDICATE))) { // 通知
                    LogUtils.d(TAG,"开始查找服务3");
                    mBluetoothLeService.getMsgBluetoothGatt().setCharacteristicNotification(
                            mReadCharacter, true);
                    BluetoothGattDescriptor descriptor = mReadCharacter.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothLeService.getMsgBluetoothGatt().writeDescriptor(descriptor);
                    mConnectRequest.connectSuccess();
                } else {
                    LogUtils.d(TAG,"开始查找服务4");
                    LogUtils.d(TAG,"注册通知失败");
                    disConnect();

                    if (mConnectRequest != null) {
                        mConnectRequest.connectFailed();
                    }
                }
            } else {
                //找不到需要的服务 连接失败，需要断开重连
                LogUtils.d(TAG,"读写服务未找到");
                disConnect();

                if (mConnectRequest != null) {
                    mConnectRequest.connectFailed();
                }
            }
        } else {
            LogUtils.d(TAG,"Service未找到");
            if (mConnectRequest != null) {
                mConnectRequest.connectFailed();
            }
        }


    }

    public void addMsg(byte[] bytes) {
        addMsg(bytes, null);
    }

    public void addMsg(byte[] bytes, BleInterface.MessageCallback mBleCallback) {
        addMsg(bytes, mBleCallback, null, null,false);
    }

    /**
     * 发消息
     *
     * @param bytes         需要发的消息  不带包头包尾
     * @param mBleCallback
     * @param timeout
     * @param timeoutResend
     */

    public void addMsg(byte[] bytes, BleInterface.MessageCallback mBleCallback, Long timeout, Boolean timeoutResend, Boolean isLongByte) {

        //包装指令
        byte[] code = null;
        if(isLongByte){
            code =bytes;
        }else {
            code = DeviceInstructionBuilder.getInstructionBytes(bytes, keyCode);
        }
        if (msgArrayList.size() > INSTRUCTION_MAX) {
            Log.d(TAG, "缓存已满" + msgArrayList.size());
//            Toast.makeText(mContext, "设备无响应", Toast.LENGTH_SHORT).show();
            return;
        }

        if (timeout == null) {
            timeout = TIMEOUT_DEFAULT;
        }

        if (timeoutResend == null) {
            timeoutResend = TIMEOUT_RESEND_DEFULT;
        }

        //实例化指令
        BleMessageEntity mBleMessage = new BleMessageEntity(code, mBleCallback, timeout, timeoutResend, keyCode);
        keyCode++;
        if (keyCode > 254) {
            keyCode = 0;
        }

        //将指令添加到指令栈里
        msgArrayList.add(mBleMessage);
        Log.d(TAG, "需要添加消息: 当前缓存" + msgArrayList.size());
        if (msgArrayList.size() == 1)
            push();
    }

    /**
     * 发送指令栈第一条指令
     */
    private synchronized void push() {
        if (!msgArrayList.isEmpty()) {
            currentMsg = msgArrayList.get(0);
            Log.d(TAG, "发消息 " + "UnicodeUtil.toHexString(String.valueOf(currentMsg))" +
                    "   缓存:" + msgArrayList.size() +
                    "   time:" + System.currentTimeMillis());
            sendMsg(currentMsg.getBytes());
            Log.d(TAG, "已发送，未循环");
            mHandler.postDelayed(bleTimeoutRunnable, currentMsg.getTimeout());//添加超时
        }
    }

    /**
     * 发送下一条指令
     */
    private void pushNext() {
        removeFirstMsg();
        push();
    }

    /**
     * 移除第一条指令
     */
    private void removeFirstMsg() {
        if (!msgArrayList.isEmpty()) {
            msgArrayList.remove(0);
        }
        repushTimes = 0;
    }

    //是否连接
    public boolean isConnected() {
        return connected;
    }

    //超时指令
    private Runnable bleTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "是否超时重发:" + currentMsg.isTimeoutReSend());
            if (currentMsg.isTimeoutReSend()) {//判断指令是否需要超时重发
                Log.d(TAG, "重发次数:" + repushTimes);
                if (repushTimes < REPUSH_MAX) {
                    push();
                    repushTimes++;
                } else {

                    if (currentMsg.getMsgRequestInterface() != null)
                        currentMsg.getMsgRequestInterface().onMessageFailed("ERROR_MSG");
                    pushNext();
                }
            } else {
                pushNext();
            }
        }
    };

    public interface ConnectionRequest {
        void connectSuccess();

        void connectFailed();
    }
}
