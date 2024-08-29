package com.octopus.android.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.zhuchao.android.ble.BLEManager;
import com.zhuchao.android.ble.OnBleConnectListener;
import com.zhuchao.android.ble.OnDeviceSearchListener;
import com.zhuchao.android.fbase.ByteUtils;
import com.zhuchao.android.fbase.eventinterface.PermissionListener;
import com.zhuchao.android.session.base.BaseActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    public static final String SERVICE_UUID = "49535343-fe7d-4ae5-8fa9-9fafd205e455";  //蓝牙通讯服务
    public static final String READ_UUID = "49535343-1e4d-4bd9-ba61-23c647249616";  //读特征
    public static final String WRITE_UUID = "49535343-8841-43f4-a8d4-ecbe34729bb3";  //写特征
    private static final String TAG = "BLEMain";
    private static final int CONNECT_SUCCESS = 0x01;
    private static final int CONNECT_FAILURE = 0x02;
    private static final int DISCONNECT_SUCCESS = 0x03;
    private static final int SEND_SUCCESS = 0x04;
    private static final int SEND_FAILURE = 0x05;
    private static final int RECEIVE_SUCCESS = 0x06;
    private static final int RECEIVE_FAILURE = 0x07;
    private static final int START_DISCOVERY = 0x08;
    private static final int STOP_DISCOVERY = 0x09;
    private static final int DISCOVERY_DEVICE = 0x0A;
    private static final int DISCOVERY_OUT_TIME = 0x0B;
    private static final int SELECT_DEVICE = 0x0C;
    private static final int BT_OPENED = 0x0D;
    private static final int BT_CLOSED = 0x0E;
    private final String[] requestPermissionArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    private List<String> deniedPermissionList = new ArrayList<>();
    private Button btSearch;
    private TextView tvCurConState;
    private TextView tvName;
    private TextView tvAddress;
    private Button btConnect;
    private Button btDisconnect;
    private EditText etSendMsg;
    private Button btSend;
    private TextView tvSendResult;
    private TextView tvReceive;
    private LinearLayout llDeviceList;
    private LinearLayout llDataSendReceive;
    private ListView lvDevices;
    private BleDeviceListAdapter bleDeviceListAdapter;
    private Context mContext;
    private BLEManager bleManager;
    private BLEBroadcastReceiver bleBroadcastReceiver;
    private BluetoothDevice curBluetoothDevice;  //当前连接的设备
    private boolean curConnState = false;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(Objects.requireNonNull(Looper.myLooper())) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case START_DISCOVERY:
                    Log.d(TAG, "开始搜索设备...");
                    break;

                case STOP_DISCOVERY:
                    Log.d(TAG, "停止搜索设备...");
                    break;

                case DISCOVERY_DEVICE:  //扫描到设备
                    ScanResult scanResult = (ScanResult) msg.obj;
                    bleDeviceListAdapter.addDevice(scanResult);

                    break;

                case SELECT_DEVICE:
                    BluetoothDevice bluetoothDevice = (BluetoothDevice) msg.obj;
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        //return;
                    }
                    tvName.setText(bluetoothDevice.getName());
                    tvAddress.setText(bluetoothDevice.getAddress());
                    curBluetoothDevice = bluetoothDevice;
                    break;

                case CONNECT_FAILURE: //连接失败
                    Log.d(TAG, "连接失败");
                    tvCurConState.setText("连接失败");
                    curConnState = false;
                    break;

                case CONNECT_SUCCESS:  //连接成功
                    Log.d(TAG, "连接成功");
                    tvCurConState.setText("连接成功");
                    curConnState = true;
                    llDataSendReceive.setVisibility(View.VISIBLE);
                    llDeviceList.setVisibility(View.GONE);
                    break;

                case DISCONNECT_SUCCESS:
                    Log.d(TAG, "断开成功");
                    tvCurConState.setText("断开成功");
                    curConnState = false;

                    break;

                case SEND_FAILURE: //发送失败
                    byte[] sendBufFail = (byte[]) msg.obj;
                    String sendFail = ByteUtils.BuffToHexStr(sendBufFail);
                    tvSendResult.setText("发送数据失败，长度" + sendBufFail.length + "--> " + sendFail);
                    break;

                case SEND_SUCCESS:  //发送成功
                    byte[] sendBufSuc = (byte[]) msg.obj;
                    String sendResult = ByteUtils.BuffToHexStr(sendBufSuc);
                    tvSendResult.setText("发送数据成功，长度" + sendBufSuc.length + "--> " + sendResult);
                    break;

                case RECEIVE_FAILURE: //接收失败
                    String receiveError = (String) msg.obj;
                    tvReceive.setText(receiveError);
                    break;

                case RECEIVE_SUCCESS:  //接收成功
                    byte[] recBufSuc = (byte[]) msg.obj;
                    String receiveResult = ByteUtils.BuffToHexStr(recBufSuc);
                    tvReceive.setText("接收数据成功，长度" + recBufSuc.length + "--> " + receiveResult);
                    break;

                case BT_CLOSED:
                    Log.d(TAG, "系统蓝牙已关闭");
                    break;

                case BT_OPENED:
                    Log.d(TAG, "系统蓝牙已打开");
                    break;
            }

        }
    };
    //扫描结果回调
    private final OnDeviceSearchListener onDeviceSearchListener = new OnDeviceSearchListener() {
        @Override
        public void onDeviceFound(ScanResult result) {
            Message message = new Message();
            message.what = DISCOVERY_DEVICE;
            message.obj = result;
            mHandler.sendMessage(message);
        }

        @Override
        public void onDiscoveryOutTime() {
            Message message = new Message();
            message.what = DISCOVERY_OUT_TIME;
            mHandler.sendMessage(message);
        }
    };
    //连接回调
    private final OnBleConnectListener onBleConnectListener = new OnBleConnectListener() {
        @Override
        public void onConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            //因为服务发现成功之后，才能通讯，所以在成功发现服务的地方表示连接成功
        }

        @Override
        public void onConnectFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String exception, int status) {
            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onDisConnecting(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice) {

        }

        @Override
        public void onDisConnectSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            Message message = new Message();
            message.what = DISCONNECT_SUCCESS;
            message.obj = status;
            mHandler.sendMessage(message);
        }

        @Override
        public void onServiceDiscoverySucceed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, int status) {
            //因为服务发现成功之后，才能通讯，所以在成功发现服务的地方表示连接成功
            Message message = new Message();
            message.what = CONNECT_SUCCESS;
            mHandler.sendMessage(message);
        }

        @Override
        public void onServiceDiscoveryFailed(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, String failMsg) {
            Message message = new Message();
            message.what = CONNECT_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceiveMessage(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, BluetoothGattCharacteristic characteristic, byte[] msg) {
            Message message = new Message();
            message.what = RECEIVE_SUCCESS;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReceiveError(String errorMsg) {
            Message message = new Message();
            message.what = RECEIVE_FAILURE;
            mHandler.sendMessage(message);
        }

        @Override
        public void onWriteSuccess(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg) {
            Message message = new Message();
            message.what = SEND_SUCCESS;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onWriteFailure(BluetoothGatt bluetoothGatt, BluetoothDevice bluetoothDevice, byte[] msg, String errorMsg) {
            Message message = new Message();
            message.what = SEND_FAILURE;
            message.obj = msg;
            mHandler.sendMessage(message);
        }

        @Override
        public void onReadRssi(BluetoothGatt bluetoothGatt, int Rssi, int status) {

        }

        @Override
        public void onMTUSetSuccess(String successMTU, int newMtu) {

        }

        @Override
        public void onMTUSetFailure(String failMTU) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = MainActivity.this;

        //动态申请权限（Android 6.0）

        //初始化视图
        initView();
        //初始化监听
        iniListener();
        //初始化数据
        initData();
        //注册广播
        initBLEBroadcastReceiver();
        //初始化权限
        initPermissions();

    }

    /**
     * 初始化视图
     */
    private void initView() {
        btSearch = findViewById(R.id.bt_search);
        tvCurConState = findViewById(R.id.tv_cur_con_state);
        btConnect = findViewById(R.id.bt_connect);
        btDisconnect = findViewById(R.id.bt_disconnect);
        tvName = findViewById(R.id.tv_name);
        tvAddress = findViewById(R.id.tv_address);
        etSendMsg = findViewById(R.id.et_send_msg);
        btSend = findViewById(R.id.bt_to_send);
        tvSendResult = findViewById(R.id.tv_send_result);
        tvReceive = findViewById(R.id.tv_receive_result);
        llDeviceList = findViewById(R.id.ll_device_list);
        llDataSendReceive = findViewById(R.id.ll_data_send_receive);
        lvDevices = findViewById(R.id.lv_devices);
    }

    /**
     * 初始化监听
     */
    private void iniListener() {
        btSearch.setOnClickListener(this);
        btConnect.setOnClickListener(this);
        btDisconnect.setOnClickListener(this);
        btSend.setOnClickListener(this);

        lvDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                BleDevice bleDevice = (BleDevice) bleDeviceListAdapter.getItem(i);
                BluetoothDevice bluetoothDevice = bleDevice.getBluetoothDevice();
                if (bleManager != null) {
                    bleManager.stopDiscoveryDevice();
                }
                Message message = new Message();
                message.what = SELECT_DEVICE;
                message.obj = bluetoothDevice;
                mHandler.sendMessage(message);
            }
        });
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //列表适配器
        bleDeviceListAdapter = new BleDeviceListAdapter(MainActivity.this);
        lvDevices.setAdapter(bleDeviceListAdapter);

        //初始化ble管理器
        bleManager = new BLEManager();
        if (!bleManager.initBle(mContext)) {
            Log.d(TAG, "该设备不支持低功耗蓝牙");
            Toast.makeText(mContext, "该设备不支持低功耗蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            if (!bleManager.isEnable()) {
                //去打开蓝牙
                bleManager.openBluetooth(mContext, false);
            }
        }
    }

    /**
     * 注册广播
     */
    private void initBLEBroadcastReceiver() {
        //注册广播接收
        bleBroadcastReceiver = new BLEBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED); //开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);//扫描结束
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//手机蓝牙状态监听
        registerReceiver(bleBroadcastReceiver, intentFilter);
    }

    /**
     * 初始化权限
     */
    private void initPermissions() {
        //Android 6.0以上动态申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestRuntimePermission(MainActivity.this, requestPermissionArray, new PermissionListener() {
                @Override
                public void onGranted() {
                    Log.d(TAG, "所有权限已被授予");
                }

                //用户勾选“不再提醒”拒绝权限后，关闭程序再打开程序只进入该方法！
                @Override
                public void onDenied(List<String> deniedPermissions) {
                    deniedPermissionList = deniedPermissions;
                    for (String deniedPermission : deniedPermissionList) {
                        Log.e(TAG, "被拒绝权限：" + deniedPermission);
                    }
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //注销广播接收
        unregisterReceiver(bleBroadcastReceiver);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.bt_search) {  //搜索蓝牙
            llDataSendReceive.setVisibility(View.GONE);
            llDeviceList.setVisibility(View.VISIBLE);
            searchBtDevice();
        } else if (id == R.id.bt_connect) { //连接蓝牙
            if (!curConnState) {
                if (bleManager != null) {
                    bleManager.connectBleDevice(mContext, curBluetoothDevice, 15000, SERVICE_UUID, READ_UUID, WRITE_UUID, onBleConnectListener);
                }
            } else {
                Toast.makeText(this, "当前设备已连接", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.bt_disconnect) { //断开连接
            if (curConnState) {
                if (bleManager != null) {
                    bleManager.disConnectDevice();
                }
            } else {
                Toast.makeText(this, "当前设备未连接", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.bt_to_send) { //发送数据
            if (curConnState) {
                String sendMsg = etSendMsg.getText().toString();
                if (sendMsg.isEmpty()) {
                    Toast.makeText(this, "发送数据为空！", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (bleManager != null) {
                    bleManager.sendMessage(sendMsg);  //以16进制字符串形式发送数据
                }
            } else {
                Toast.makeText(this, "请先连接当前设备", Toast.LENGTH_SHORT).show();
            }
        }

    }

    //////////////////////////////////  搜索设备  /////////////////////////////////////////////////
    private void searchBtDevice() {
        if (bleManager == null) {
            Log.d(TAG, "searchBtDevice()-->bleManager == null");
            return;
        }

        if (bleManager.isDiscovery()) { //当前正在搜索设备...
            bleManager.stopDiscoveryDevice();
        }

        if (bleDeviceListAdapter != null) {
            bleDeviceListAdapter.clear();  //清空列表
        }

        //开始搜索
        bleManager.startDiscoveryDevice(onDeviceSearchListener, 15000);
    }

    /**
     * 蓝牙广播接收器
     */
    private class BLEBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_STARTED)) { //开启搜索
                Message message = new Message();
                message.what = START_DISCOVERY;
                mHandler.sendMessage(message);

            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {//完成搜素
                Message message = new Message();
                message.what = STOP_DISCOVERY;
                mHandler.sendMessage(message);
            } else if (TextUtils.equals(action, BluetoothAdapter.ACTION_STATE_CHANGED)) {   //系统蓝牙状态监听

                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Message message = new Message();
                    message.what = BT_CLOSED;
                    mHandler.sendMessage(message);
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Message message = new Message();
                    message.what = BT_OPENED;
                    mHandler.sendMessage(message);
                }
            }
        }
    }
}
