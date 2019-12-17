package com.example.jiesean.bledemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private String TAG = "MainActivity";

    //目标设备的名称，可根据自己目标设备的不同去修改该名称来完成连接
    private String mTargetDeviceName = "honor band A1";

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private List<BluetoothGattService> mServiceList;
    private ScanCallback mScanCallback;
    private boolean mScanning;
    private Handler handler;

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mStartScanBtn = (Button) findViewById(R.id.startScanBtn);
        mStartScanBtn.setOnClickListener(this);

        handler = new Handler();

        if (Build.VERSION.SDK_INT >= 21) {
            mScanCallback = new MyScanCallback();
        }

        checkBluetoothPermission();
        initBluetooth();
    }

    /**
     * enable bluetooth
     */
    private void initBluetooth() {
        //get Bluetooth service
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //get Bluetooth Adapter
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {//platform not support bluetooth
            Log.d(TAG, "Bluetooth is not support");
        } else {
            int status = mBluetoothAdapter.getState();
            //bluetooth is disabled
            if (status == BluetoothAdapter.STATE_OFF) {
                // enable bluetooth
                mBluetoothAdapter.enable();
            }
        }
    }

    /**
     * start Ble scan，扫描按钮的处理函数
     */
    public void startScanLeDevices() {
        if (Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 21) {
            //Android 4.3以上，Android 5.0以下
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else if (Build.VERSION.SDK_INT >= 21) {
            //Android 5.0以上，扫描的结果在mScanCallback中进行处理
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mBluetoothLeScanner.startScan(mScanCallback);
        }
        // Stops scanning after a pre-defined scan period.
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "scan timeout");
                stopScanLeDevices();
            }
        }, SCAN_PERIOD);
        mScanning = true;
    }

    public void stopScanLeDevices() {
        if (Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 21) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        } else if (Build.VERSION.SDK_INT >= 21) {
            mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
        mScanning = false;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            Log.d(TAG, "BluetoothDevice  name=" + device.getName() + " address=" + device.getAddress());
            if (mTargetDeviceName.equals(device.getName())) {
                handler.removeCallbacksAndMessages(null);
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                device.connectGatt(MainActivity.this, false, mGattCallback);
            }

        }
    };

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.startScanBtn) {
            startScanLeDevices();
        }
    }

    /**
     * LE设备扫描结果返回
     */
    private class MyScanCallback extends ScanCallback {

        /**
         * 扫描结果的回调，每次扫描到一个设备，就调用一次。
         *
         * @param callbackType
         * @param result
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.d(TAG, "onScanResult");
            if (result != null) {
                Log.d(TAG, "扫描到设备：" + result.getDevice().getName() + "  " + result.getDevice().getAddress());

                //此处，我们尝试连接MI 设备
                if (result.getDevice().getName() != null && mTargetDeviceName.equals(result.getDevice().getName())) {
                    //扫描到我们想要的设备后，立即停止扫描
                    handler.removeCallbacksAndMessages(null);
                    result.getDevice().connectGatt(MainActivity.this, false, mGattCallback);
                    mBluetoothLeScanner.stopScan(mScanCallback);
                }
            }
        }
    }

    /**
     * gatt连接结果的返回
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote GATT server
         *
         * @param gatt 返回连接建立的gatt对象
         * @param status 返回的是此次gatt操作的结果，成功了返回0
         * @param newState 每次client连接或断开连接状态变化，STATE_CONNECTED 0，STATE_CONNECTING 1,STATE_DISCONNECTED 2,STATE_DISCONNECTING 3
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.d(TAG, "onConnectionStateChange status:" + status + "  newState:" + newState);
            if (status == 0) {
                gatt.discoverServices();
            }
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt 返回的是本次连接的gatt对象
         * @param status
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "onServicesDiscovered status" + status);
            mServiceList = gatt.getServices();
            if (mServiceList != null) {
                Log.d(TAG, "mServiceList=" + mServiceList);
                Log.d(TAG, "Services num:" + mServiceList.size());
            }

            for (BluetoothGattService service : mServiceList) {
                List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                Log.d(TAG, "扫描到Service：" + service.getUuid());

                for (BluetoothGattCharacteristic characteristic : characteristics) {
                    Log.d(TAG, "characteristic: " + characteristic.getUuid());
                }
            }
            // 读取手机模拟BLE的特征值
//            Log.d(TAG, "onServicesDiscovered: readCharacteristic: " +
//                    gatt.readCharacteristic(gatt.getService(UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb")).
//                            getCharacteristic(UUID.fromString("00002A19-0000-1000-8000-00805f9b34fb"))));

            // 调戏华为手环
//            BluetoothGattCharacteristic temp = gatt.getService(UUID.fromString("6e400000-b5a3-f393-e0a9-e50e24dcca9d")).
//                    getCharacteristic(UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9d"));
//            temp.setValue(new byte[]{63});
//            gatt.writeCharacteristic(temp);
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt
         * @param characteristic
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged");
        }

        /**
         * Callback indicating the result of a characteristic write operation.
         *
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicWrite");
        }

        /**
         *Callback reporting the result of a characteristic read operation.
         *
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead: status=" + status);
            Log.d(TAG, "onCharacteristicRead: UUID=" + characteristic.getUuid());
            byte[] data = characteristic.getValue();
            Log.d(TAG, "onCharacteristicRead: value=" + Arrays.toString(data));
        }

        /**
         * Callback indicating the result of a descriptor write operation.
         *
         * @param gatt
         * @param descriptor
         * @param status
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite");
        }

    };


    /**
     * 检查蓝牙权限
     */
    private void checkBluetoothPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //校验是否已具有模糊定位权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
            }
        } else {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //权限被用户同意,做相应的事情
                Log.i(TAG, "onRequestPermissionsResult: 权限被用户同意");
            } else {
                //权限被用户拒绝，做相应的事情
                Log.i(TAG, "onRequestPermissionsResult: 权限被用户拒绝");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopScanLeDevices();
    }
}
