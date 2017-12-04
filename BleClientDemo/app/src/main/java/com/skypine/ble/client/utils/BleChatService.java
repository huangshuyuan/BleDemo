package com.skypine.ble.client.utils;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import com.skypine.ble.client.ConnectionState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Author: syhuang
 * Date:  2017/12/4
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleChatService {

    public static final  String TAG               = BleChatService.class.getSimpleName();
    private final static int    READ_MESSAGE      = 1002;
    public final static  int    REQUEST_ENABLE_BT = 1001;
    public final static  int    TOAST_STATE       = 1003;
    // Constants that indicate the current connection state
    public static final  int    STATE_NONE        = 0;       // we're doing nothing
    public static final  int    STATE_LISTEN      = 1;     // now listening for incoming connections
    public static final  int    STATE_CONNECTING  = 2; // now initiating an outgoing connection
    public static final  int    STATE_CONNECTED   = 3;  // now connected to a remote device

    private static BluetoothGattCharacteristic characteristicRead;
    private static BluetoothGattCharacteristic characteristicWrite;

    private static UUID UUID_SERVER     = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_READ       = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_WRITE      = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private static int              mState;//连接状态
    private static BleChatService   instance;
    private static BluetoothAdapter mBluetoothAdapter;
    public static  BluetoothGatt    mBluetoothGatt;//gatt连接设备
    private static Context          mContext;

    //扫描到的设备
    private static List<BluetoothDevice> mBluetoothDeviceList = new ArrayList<>();

    public static int getState() {
        return mState;
    }

    public static void setState(int state) {
        mState = state;
    }

    static BluetoothDeviceListener mBluetoothDeviceListener;

    public static BluetoothDeviceListener getmBluetoothDeviceListener() {
        return mBluetoothDeviceListener;
    }

    public static void setmBluetoothDeviceListener(BluetoothDeviceListener mBluetoothDeviceListener) {
        BleChatService.mBluetoothDeviceListener = mBluetoothDeviceListener;
    }

    public interface BluetoothDeviceListener {
        /**
         * 扫描中
         */
        public abstract void onReceive(List<BluetoothDevice> mBluetoothDeviceList);
    }


    public static synchronized BleChatService getInstance(Context context) {
        if (instance == null) {
            instance = new BleChatService(context);
        }
        return instance;
    }

    /**
     * ble
     */

    public void bleScan() {
        mBluetoothDeviceList.clear();
        scanLeDevice(true);
        Log.e(TAG, "scan");
    }

    boolean mScanning   = false;
    int     SCAN_PERIOD = 1000;

    /**
     * 定时扫描
     *
     * @param enable
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            // 预先定义停止蓝牙扫描的时间（因为蓝牙扫描需要消耗较多的电量）
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        mBluetoothAdapter.stopLeScan(callback);
                    }
                }
            }, SCAN_PERIOD);
            mScanning = true;

            // 定义一个回调接口供扫描结束处理
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBluetoothAdapter.startLeScan(callback);
            }
        } else {
            mScanning = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mBluetoothAdapter.stopLeScan(callback);
            }
        }
    }

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     */
    public BleChatService(Context context) {
        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            Activity activity = (Activity) context;
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //在activity里面需要回调onActivityResult-----------------
        mState = STATE_NONE;
        mContext = context;
    }

    /**
     * 连接设备
     *
     * @param connectGatt
     */
    public static void connect(BluetoothDevice connectGatt) {
        mBluetoothAdapter.stopLeScan(callback);
        mBluetoothGatt = connectGatt.connectGatt(mContext, true, mBluetoothGattCallback);
    }

    /**
     * 连接状态改变
     */
    private static BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.e(TAG, "onConnectionStateChange: thread "
                    + Thread.currentThread() + " status " + newState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    //连接中断
                    String err = "Cannot connect device with error status: " + status;
                    // 当尝试连接失败的时候调用 disconnect 方法是不会引起这个方法回调的，所以这里
                    //   直接回调就可以了。
                    setState(STATE_NONE);
                    mHandler.obtainMessage(TOAST_STATE, "disconnect!").sendToTarget();
                    Log.e(TAG, err);
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.e(TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt.discoverServices());
                    Log.e(TAG, "connect--->success" + newState + "," + gatt.getServices().size());
                    setState(STATE_CONNECTING);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "Disconnected from GATT server.");

                    Log.e(TAG, "connect--->failed" + newState);
                    setState(STATE_NONE);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(TOAST_STATE, "start connect ...").sendToTarget();
                Log.e(TAG, "onServicesDiscovered received:  SUCCESS");
                initCharacteristic();
                try {
                    Thread.sleep(200);//延迟发送，否则第一次消息会不成功
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "onServicesDiscovered error falure " + status);
                setState(STATE_NONE);
            }

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e(TAG, "onCharacteristicWrite status: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(TOAST_STATE, "connect success!").sendToTarget();
                setState(STATE_CONNECTED);
            } else {
                setState(STATE_NONE);
            }
            Log.e(TAG, "onDescriptorWrite status-->: " + status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            Log.e(TAG, "onDescriptorRead status: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.e(TAG, "onCharacteristicRead status: " + status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "onCharacteristicChanged characteristic: " + characteristic);
            readCharacteristic(characteristic);
        }

    };

    public static synchronized void initCharacteristic() {
        if (mBluetoothGatt == null)
            throw new NullPointerException();
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
        Log.e(TAG, services.toString());
        if (services.size() > 0) {
            Log.e(TAG, " characteristic" + services.size());
            BluetoothGattService service = mBluetoothGatt.getService(UUID_SERVER);
            characteristicRead = service.getCharacteristic(UUID_READ);
            characteristicWrite = service.getCharacteristic(UUID_WRITE);

            if (characteristicRead == null)
                throw new NullPointerException();
            if (characteristicWrite == null)
                throw new NullPointerException();
            mBluetoothGatt.setCharacteristicNotification(characteristicRead, true);
            BluetoothGattDescriptor descriptor = characteristicRead.getDescriptor(UUID_DESCRIPTOR);
            if (descriptor == null)
                throw new NullPointerException();
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        } else {
            Log.e(TAG, "no characteristic");
        }
        Log.e(TAG, "init characteristic");

    }

    public static void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        byte[] bytes = characteristic.getValue();
        mHandler.obtainMessage(READ_MESSAGE, bytes).sendToTarget();
        Log.e(TAG, "readCharacteristic,length--->: " + bytes.length);
    }

    static Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case READ_MESSAGE:
                    byte[] read = (byte[]) msg.obj;
                    String readMessage = new String(read, 0, read.length);
                    Log.e(TAG, "read--->" + readMessage);
                    if (read != null)
                        for (ReceiveListener receiveListener : ReceiverUtil.getListenerArrayList()) {
                            receiveListener.onReceive(readMessage);
                        }
                    break;
                case TOAST_STATE:
                    Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    /**
     * 写数据
     *
     * @param cmd
     */


    public static void write(byte[] cmd) {
        if (cmd == null || cmd.length == 0)
            return;
        if (getState() == STATE_CONNECTED) {
            //        synchronized (LOCK) {
            characteristicWrite.setValue(cmd);
            characteristicWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            mBluetoothGatt.writeCharacteristic(characteristicWrite);
            Log.e(TAG, "write:--->" + new String(cmd));
        } else {
            Log.e(TAG, "no connected");
            mHandler.obtainMessage(TOAST_STATE, "no connected").sendToTarget();

        }
        //        }
    }

    /**
     * 扫描设备回调
     */
    private static final BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (device.getName() != null) {
                mBluetoothDeviceList.add(device);
                mBluetoothDeviceListener.onReceive(mBluetoothDeviceList);
            }
            Log.e(TAG, "run: scanning..." + device.getName() + "," + device.getAddress());
        }
    };

    /**
     * 关闭
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        setState(ConnectionState.STATE_NONE);
    }
}
