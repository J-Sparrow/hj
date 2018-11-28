package com.gizwits.opensource.appkit.ControlModule;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.gizwits.gizwifisdk.api.GizWifiDevice;
import com.gizwits.gizwifisdk.enumration.GizWifiDeviceNetStatus;
import com.gizwits.gizwifisdk.enumration.GizWifiErrorCode;
import com.gizwits.opensource.appkit.BleModule.BleAdapter;
import com.gizwits.opensource.appkit.BleModule.SearchActivity;
import com.gizwits.opensource.appkit.BleModule.Util.DeviceControlUtil;
import com.gizwits.opensource.appkit.BleModule.bluetooth.BleInterface;
import com.gizwits.opensource.appkit.BleModule.bluetooth.CHCarBleClient;
import com.gizwits.opensource.appkit.BleModule.entity.BleDeviceEntity;
import com.gizwits.opensource.appkit.CommonModule.GosDeploy;
import com.gizwits.opensource.appkit.GosApplication;
import com.gizwits.opensource.appkit.R;
import com.gizwits.opensource.appkit.utils.HexStrUtils;
import com.gizwits.opensource.appkit.view.HexWatcher;

public class GosDeviceControlActivity extends GosControlModuleBaseActivity
		implements OnClickListener, OnEditorActionListener, OnSeekBarChangeListener,BleInterface.MessageCallback {

	/** 设备列表传入的设备变量 */
	private GizWifiDevice mDevice;

	private TextView tv_data_airHumi;
	private TextView tv_data_soilHumi;
	private TextView tv_data_illumination;
	private TextView tv_data_airTemp;
	private TextView tv_data_soilTemp;
	private TextView tv_data_co2Conc;
	private static final int SEARCHACTIVITY_CONECT = 2;
	private Button light,hot,water,fan;



	private static final String TAG = "SearchActivity";
	private BluetoothAdapter mBluetoothAdapter;
	private List<BleDeviceEntity> list = new ArrayList<>();
	private List<BleDeviceEntity> bluetoothList = new ArrayList<>();

	private BleAdapter bleAdapter;
	private ListView listView;
	private ImageView button;


	private final int CONNECTED = 0;
	private final int DISCONNECTED = 1;

	private enum handler_key {

		/** 更新界面 */
		UPDATE_UI,

		DISCONNECT,
	}

	private Runnable mRunnable = new Runnable() {
		public void run() {
			if (isDeviceCanBeControlled()) {
				progressDialog.cancel();
			} else {
				toastDeviceNoReadyAndExit();
			}
		}

	};

	/** The handler. */
	Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			handler_key key = handler_key.values()[msg.what];
			switch (key) {
				case UPDATE_UI:
					updateUI();
					break;
				case DISCONNECT:
					toastDeviceDisconnectAndExit();
					break;
			}
		}
	};

	MyHandler myHandler = new MyHandler();
	class MyHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case CONNECTED:
					Log.d(TAG,"连接好了");
					Toast.makeText(GosDeviceControlActivity.this, "连接成功", Toast.LENGTH_LONG).show();
					break;
				case DISCONNECTED:
					Log.d(TAG,"连接断开");
					Toast.makeText(GosDeviceControlActivity.this, "连接失败", Toast.LENGTH_LONG).show();
					handler.sendEmptyMessage(111);
					break;
				default:
					break;
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
				if( bluetoothList.size() == 0){
					return false;
				}
				final BleDeviceEntity ble = bluetoothList.get(0);
				BluetoothDevice device = ble.getDevice();
				Toast.makeText(getApplicationContext(), R.string.connecting_please_wait_a_moment, Toast.LENGTH_LONG);
				try {
					Log.d("好", device.getAddress());
					Log.d("好", "" + GosApplication.getmBleClient());

					Boolean isSucess =  GosApplication.getmBleClient().connect(device.getAddress());
					if(!isSucess){
						myHandler.sendEmptyMessage(DISCONNECTED);
						Log.d("","连接动作失败，1");
					}

					GosApplication.getmBleClient().setmConnectRequest(new CHCarBleClient.ConnectionRequest() {
						@Override
						public void connectSuccess() {
							myHandler.sendEmptyMessage(CONNECTED);
						}

						@Override
						public void connectFailed() {
							myHandler.sendEmptyMessage(DISCONNECTED);
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return true;
		}
	});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_gos_device_control);
		initDevice();
		setToolBar(true, getDeviceName());
		final Drawable add = getResources().getDrawable(R.drawable.common_setting_more);
		int color = GosDeploy.appConfig_Contrast();
		add.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		mToolbar.setOverflowIcon(add);
		initView();
		initEvent();
	}

	private void initView() {

		initBluetooth();
		tv_data_airHumi = (TextView) findViewById(R.id.tv_data_airHumi);
		tv_data_soilHumi = (TextView) findViewById(R.id.tv_data_soilHumi);
		tv_data_illumination = (TextView) findViewById(R.id.tv_data_illumination);
		tv_data_airTemp = (TextView) findViewById(R.id.tv_data_airTemp);
		tv_data_soilTemp = (TextView) findViewById(R.id.tv_data_soilTemp);
		tv_data_co2Conc = (TextView) findViewById(R.id.tv_data_co2Conc);

		Button bleSearch = (Button) findViewById(R.id.ble);
		bleSearch.setOnClickListener(this);
		bleSearch.setVisibility(View.GONE);
		water = (Button) findViewById(R.id.water);
		water.setOnClickListener(this);
		fan = (Button) findViewById(R.id.fan);
		fan.setOnClickListener(this);
		hot = (Button) findViewById(R.id.hot);
		hot.setOnClickListener(this);
		light = (Button) findViewById(R.id.light);
		light.setOnClickListener(this);


	}

	private void initEvent() {


	}

	private void initDevice() {
		Intent intent = getIntent();
		mDevice = (GizWifiDevice) intent.getParcelableExtra("GizWifiDevice");
		mDevice.setListener(gizWifiDeviceListener);
		Log.i("Apptest", mDevice.getDid());
	}

	private String getDeviceName() {
		if (TextUtils.isEmpty(mDevice.getAlias())) {
			return mDevice.getProductName();
		}
		return mDevice.getAlias();
	}

	@Override
	protected void onResume() {
		super.onResume();
		getStatusOfDevice();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mHandler.removeCallbacks(mRunnable);
		mBluetoothAdapter.cancelDiscovery(); //取消扫描
		unregisterReceiver(mBluetoothReceiver);
		// 退出页面，取消设备订阅
		mDevice.setSubscribe(false);
		mDevice.setListener(null);
	}

	private boolean isLight =false;
	private boolean isWater =false;
	private boolean isHot =false;
	private boolean isFan =false;


	@Override
	public void onClick(View v) {

//		if (!GosApplication.getmBleClient().isConnected()) {
//			Log.d(TAG, "当前蓝牙未连接");
//			Toast.makeText(GosApplication.getInstance(), "设备未成功连接", Toast.LENGTH_LONG).show();
//			return;
//		}

		switch (v.getId()) {
			case  R.id.ble:
				Intent intent1 = new Intent(GosDeviceControlActivity.this, SearchActivity.class);
				startActivityForResult(intent1, SEARCHACTIVITY_CONECT);
				break;
			case R.id.light:
				if (isLight){
					DeviceControlUtil.closeLightAll(this,GosDeviceControlActivity.this);
					isLight = false;
					light.setText("开灯");
				}else{
					DeviceControlUtil.greenLightAll(this, this);
					isLight = true;
					light.setText("关灯");
				}
				break;
			case R.id.water:
				if (isWater){
					DeviceControlUtil.motor("01","01",0,this, this);
					isWater = false;
					water.setText("浇水肥");
				}else{
					DeviceControlUtil.motor("01","01",255,this, this);
					isWater = true;
					water.setText("停止浇水肥");
				}
				break;
			case R.id.fan:
				if (isFan){
					DeviceControlUtil.motor("02","01",0,this, this);
					isFan = false;
					fan.setText("开风扇");
				}else{
					DeviceControlUtil.motor("02","01",255,this, this);
					isFan = true;
					fan.setText("关风扇");
				}
				break;
			case R.id.hot:
				if (isHot){
					DeviceControlUtil.motor("03","01",0,this, this);
					isHot = false;
					hot.setText("加热");
				}else{
					DeviceControlUtil.motor("03","01",255,this, this);
					isHot = true;
					hot.setText("取消加热");
				}
				break;
			default:
				break;
		}
	}


	/*
	 * ========================================================================
	 * EditText 点击键盘“完成”按钮方法
	 * ========================================================================
	 */
	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

		switch (v.getId()) {
			default:
				break;
		}
		hideKeyBoard();
		return false;

	}

	/*
	 * ========================================================================
	 * seekbar 回调方法重写
	 * ========================================================================
	 */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

		switch (seekBar.getId()) {
			default:
				break;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {

	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		switch (seekBar.getId()) {
			default:
				break;
		}
	}

	/*
	 * ========================================================================
	 * 菜单栏
	 * ========================================================================
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.device_more, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

			case R.id.action_setDeviceInfo:
				setDeviceInfo();
				break;

			case R.id.action_getHardwareInfo:
				if (mDevice.isLAN()) {
					mDevice.getHardwareInfo();
				} else {
					myToast("只允许在局域网下获取设备硬件信息！");
				}
				break;

			case R.id.action_getStatu:
				mDevice.getDeviceStatus();
				break;

			default:
				break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Description:根据保存的的数据点的值来更新UI
	 */
	protected void updateUI() {

		tv_data_airHumi.setText(data_airHumi+"");
		tv_data_soilHumi.setText(data_soilHumi+"");
		tv_data_illumination.setText(data_illumination+"");
		tv_data_airTemp.setText(data_airTemp+"");
		tv_data_soilTemp.setText(data_soilTemp+"");
		tv_data_co2Conc.setText(data_co2Conc+"");
	}

	private void setEditText(EditText et, Object value) {
		et.setText(value.toString());
		et.setSelection(value.toString().length());
		et.clearFocus();
	}

	/**
	 * Description:页面加载后弹出等待框，等待设备可被控制状态回调，如果一直不可被控，等待一段时间后自动退出界面
	 */
	private void getStatusOfDevice() {
		// 设备是否可控
		if (isDeviceCanBeControlled()) {
			// 可控则查询当前设备状态
			mDevice.getDeviceStatus();
		} else {
			// 显示等待栏
			progressDialog.show();
			if (mDevice.isLAN()) {
				// 小循环10s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 10000);
			} else {
				// 大循环20s未连接上设备自动退出
				mHandler.postDelayed(mRunnable, 20000);
			}
		}
	}

	/**
	 * 发送指令,下发单个数据点的命令可以用这个方法
	 *
	 * <h3>注意</h3>
	 * <p>
	 * 下发多个数据点命令不能用这个方法多次调用，一次性多次调用这个方法会导致模组无法正确接收消息，参考方法内注释。
	 * </p>
	 *
	 * @param key
	 *            数据点对应的标识名
	 * @param value
	 *            需要改变的值
	 */
	private void sendCommand(String key, Object value) {
		if (value == null) {
			return;
		}
		int sn = 5;
		ConcurrentHashMap<String, Object> hashMap = new ConcurrentHashMap<String, Object>();
		hashMap.put(key, value);
		// 同时下发多个数据点需要一次性在map中放置全部需要控制的key，value值
		// hashMap.put(key2, value2);
		// hashMap.put(key3, value3);
		mDevice.write(hashMap, sn);
		Log.i("liang", "下发命令：" + hashMap.toString());
	}

	private boolean isDeviceCanBeControlled() {
		return mDevice.getNetStatus() == GizWifiDeviceNetStatus.GizDeviceControlled;
	}

	private void toastDeviceNoReadyAndExit() {
		Toast.makeText(this, "设备无响应，请检查设备是否正常工作", Toast.LENGTH_SHORT).show();
		finish();
	}

	private void toastDeviceDisconnectAndExit() {
		Toast.makeText(GosDeviceControlActivity.this, "连接已断开", Toast.LENGTH_SHORT).show();
		finish();
	}

	/**
	 * 展示设备硬件信息
	 *
	 * @param hardwareInfo
	 */
	private void showHardwareInfo(String hardwareInfo) {
		String hardwareInfoTitle = "设备硬件信息";
		new AlertDialog.Builder(this).setTitle(hardwareInfoTitle).setMessage(hardwareInfo)
				.setPositiveButton(R.string.besure, null).show();
	}

	/**
	 * Description:设置设备别名与备注
	 */
	private void setDeviceInfo() {

		final Dialog mDialog = new AlertDialog.Builder(this,R.style.edit_dialog_style).setView(new EditText(this)).create();
		mDialog.show();

		Window window = mDialog.getWindow();
		window.setContentView(R.layout.alert_gos_set_device_info);
		WindowManager.LayoutParams layoutParams = window.getAttributes();
		layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		window.setAttributes(layoutParams);
		final EditText etAlias;
		final EditText etRemark;
		etAlias = (EditText) window.findViewById(R.id.etAlias);
		etRemark = (EditText) window.findViewById(R.id.etRemark);

		LinearLayout llNo, llSure;
		llNo = (LinearLayout) window.findViewById(R.id.llNo);
		llSure = (LinearLayout) window.findViewById(R.id.llSure);

		if (!TextUtils.isEmpty(mDevice.getAlias())) {
			setEditText(etAlias, mDevice.getAlias());
		}
		if (!TextUtils.isEmpty(mDevice.getRemark())) {
			setEditText(etRemark, mDevice.getRemark());
		}

		llNo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				mDialog.dismiss();
			}
		});

		llSure.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(etRemark.getText().toString())
						&& TextUtils.isEmpty(etAlias.getText().toString())) {
					myToast("请输入设备别名或备注！");
					return;
				}
				mDevice.setCustomInfo(etRemark.getText().toString(), etAlias.getText().toString());
				mDialog.dismiss();
				String loadingText = (String) getText(R.string.loadingtext);
				progressDialog.setMessage(loadingText);
				progressDialog.show();
			}
		});

		mDialog.setOnDismissListener(new OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				hideKeyBoard();
			}
		});
	}

	/*
	 * 获取设备硬件信息回调
	 */
	@Override
	protected void didGetHardwareInfo(GizWifiErrorCode result, GizWifiDevice device,
									  ConcurrentHashMap<String, String> hardwareInfo) {
		super.didGetHardwareInfo(result, device, hardwareInfo);
		StringBuffer sb = new StringBuffer();
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS != result) {
			myToast("获取设备硬件信息失败：" + result.name());
		} else {
			sb.append("Wifi Hardware Version:" + hardwareInfo.get(WIFI_HARDVER_KEY) + "\r\n");
			sb.append("Wifi Software Version:" + hardwareInfo.get(WIFI_SOFTVER_KEY) + "\r\n");
			sb.append("MCU Hardware Version:" + hardwareInfo.get(MCU_HARDVER_KEY) + "\r\n");
			sb.append("MCU Software Version:" + hardwareInfo.get(MCU_SOFTVER_KEY) + "\r\n");
			sb.append("Wifi Firmware Id:" + hardwareInfo.get(WIFI_FIRMWAREID_KEY) + "\r\n");
			sb.append("Wifi Firmware Version:" + hardwareInfo.get(WIFI_FIRMWAREVER_KEY) + "\r\n");
			sb.append("Product Key:" + "\r\n" + hardwareInfo.get(PRODUCT_KEY) + "\r\n");

			// 设备属性
			sb.append("Device ID:" + "\r\n" + mDevice.getDid() + "\r\n");
			sb.append("Device IP:" + mDevice.getIPAddress() + "\r\n");
			sb.append("Device MAC:" + mDevice.getMacAddress() + "\r\n");
		}
		showHardwareInfo(sb.toString());
	}

	/*
	 * 设置设备别名和备注回调
	 */
	@Override
	protected void didSetCustomInfo(GizWifiErrorCode result, GizWifiDevice device) {
		super.didSetCustomInfo(result, device);
		if (GizWifiErrorCode.GIZ_SDK_SUCCESS == result) {
			myToast("设置成功");
			progressDialog.cancel();
			finish();
		} else {
			myToast("设置失败：" + result.name());
		}
	}

	/*
	 * 设备状态改变回调，只有设备状态为可控才可以下发控制命令
	 */
	@Override
	protected void didUpdateNetStatus(GizWifiDevice device, GizWifiDeviceNetStatus netStatus) {
		super.didUpdateNetStatus(device, netStatus);
		if (netStatus == GizWifiDeviceNetStatus.GizDeviceControlled) {
			mHandler.removeCallbacks(mRunnable);
			progressDialog.cancel();
		} else {
			mHandler.sendEmptyMessage(handler_key.DISCONNECT.ordinal());
		}
	}

	/*
	 * 设备上报数据回调，此回调包括设备主动上报数据、下发控制命令成功后设备返回ACK
	 */
	@Override
	protected void didReceiveData(GizWifiErrorCode result, GizWifiDevice device,
								  ConcurrentHashMap<String, Object> dataMap, int sn) {
		super.didReceiveData(result, device, dataMap, sn);
		Log.i("liang", "接收到数据");
		if (result == GizWifiErrorCode.GIZ_SDK_SUCCESS && dataMap.get("data") != null) {
			getDataFromReceiveDataMap(dataMap);
			mHandler.sendEmptyMessage(handler_key.UPDATE_UI.ordinal());
		}
	}
	@Override
	public void onMessageReceive(String data) {

	}

	@Override
	public void onMessageFailed(String msg) {

	}

	private void initBluetooth() {

		//申请定位权限，才能用蓝牙
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 123);
			}
		}
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
				Log.d("麦克",mac);


				if ("C4:F3:12:50:FC:AF".equals(mac)) {
					list.add(new BleDeviceEntity(ibeaconName, mac, rssi, scanDevice));
					Log.d("麦克",mac);
					handler.sendEmptyMessage(111);
				}

			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
			}
		}
	};



}