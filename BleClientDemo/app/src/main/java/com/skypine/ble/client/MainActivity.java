package com.skypine.ble.client;

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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = MainActivity.class.getSimpleName();
    private BluetoothAdapter mBluetoothAdapter;
    private final int    REQUEST_ENABLE_BT = 1001;
    private       String mac               = "米6";

    private BluetoothGattCharacteristic characteristicRead;
    private BluetoothGattCharacteristic characteristicWrite;
    private BluetoothDevice             connectGatt;

    private static UUID uuidServer     = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb");
    private static UUID uuidCharRead   = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    private static UUID uuidCharWrite  = UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb");
    private static UUID uuidDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private final static int READ_MESSAGE = 1002;
    MyBlueAdapter mMyBlueAdapter;
    ListView      blueList;
    TextView      readText;
    EditText      sendText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        readText = (TextView) findViewById(R.id.read);
        sendText = (EditText) findViewById(R.id.send_edit);
        blueList = (ListView) findViewById(R.id.list);

        // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        }


        findViewById(R.id.scan).setOnClickListener(this);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.send).setOnClickListener(this);
        mMyBlueAdapter = new MyBlueAdapter();
        blueList.setAdapter(mMyBlueAdapter);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan:
                bleScan();
                break;
            case R.id.connect:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    mBluetoothAdapter.stopLeScan(callback);
                    mBluetoothGatt = connectGatt.connectGatt(MainActivity.this, true, mBluetoothGattCallback);
                }
                break;
            case R.id.send:
                byte[] sendValue = sendText.getText().toString().trim().getBytes();
                Log.e(TAG, new String(sendValue));
                try {
                    write(sendValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
    }


    /**
     * ble
     */

    private void bleScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            scanLeDevice(true);
            //            mBluetoothAdapter.startLeScan(callback);
        }
    }

    int state = ConnectionState.STATE_NONE;

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public BluetoothGatt mBluetoothGatt;

    //    状态改变
    BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            Log.e(TAG, "onConnectionStateChange: thread "
                    + Thread.currentThread() + " status " + newState);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    String err = "Cannot connect device with error status: " + status;
                    // 当尝试连接失败的时候调用 disconnect 方法是不会引起这个方法回调的，所以这里
                    //   直接回调就可以了。
                    gatt.close();
                    Log.e(TAG, err);
                    return;
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.e(TAG, "Attempting to start service discovery:" +
                            mBluetoothGatt.discoverServices());
                    Log.e(TAG, "connect--->success" + newState + "," + gatt.getServices().size());
                    setState(ConnectionState.STATE_CONNECTING);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e(TAG, "Disconnected from GATT server.");

                    Log.e(TAG, "connect--->failed" + newState);
                    setState(ConnectionState.STATE_NONE);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onServicesDiscovered received:  SUCCESS");
                setState(ConnectionState.STATE_CONNECTED);
                initCharacteristic();
                try {
                    Thread.sleep(200);//延迟发送，否则第一次消息会不成功
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "onServicesDiscovered error falure " + status);
                setState(ConnectionState.STATE_NONE);
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
            Log.e(TAG, "onDescriptorWrite status: " + status);
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


    public synchronized void initCharacteristic() {
        if (mBluetoothGatt == null)
            throw new NullPointerException();
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
        Log.e(TAG, services.toString());
        BluetoothGattService service = mBluetoothGatt.getService(uuidServer);
        characteristicRead = service.getCharacteristic(uuidCharRead);
        characteristicWrite = service.getCharacteristic(uuidCharWrite);

        if (characteristicRead == null)
            throw new NullPointerException();
        if (characteristicWrite == null)
            throw new NullPointerException();
        mBluetoothGatt.setCharacteristicNotification(characteristicRead, true);
        BluetoothGattDescriptor descriptor = characteristicRead.getDescriptor(uuidDescriptor);
        if (descriptor == null)
            throw new NullPointerException();
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);

        printf("## characteristic4Read = " + characteristicRead);
        printf("## characteristic4Write = " + characteristicWrite);
        printf("## descriptor = " + descriptor);
    }

    private void printf(String s) {
        Log.e(TAG, s);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
        byte[] bytes = characteristic.getValue();
        String str = new String(bytes);
        mHandler.obtainMessage(READ_MESSAGE, str).sendToTarget();
        Log.e(TAG, "## readCharacteristic, 读取到: " + str);
    }

    public void write(byte[] cmd) {
        Log.e(TAG, "write:" + new String(cmd));
        if (cmd == null || cmd.length == 0)
            return;
        //        synchronized (LOCK) {
        characteristicWrite.setValue(cmd);
        characteristicWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        mBluetoothGatt.writeCharacteristic(characteristicWrite);
        Log.e(TAG, "write:--->" + new String(cmd));
        //        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    List<BluetoothDevice> mBluetoothDeviceList = new ArrayList<>();

    final BluetoothAdapter.LeScanCallback callback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {

            if (device.getName() != null) {
                mBluetoothDeviceList.add(device);
                mMyBlueAdapter.notifyDataSetChanged();
            }
            //            if (device.getName().equals(mac)) {
            //                connectGatt = device;
            //                Log.e(TAG, "run: scanning..." + device.getName() + "," + device.getAddress());
            //            }

            Log.e(TAG, "run: scanning..." + device.getName() + "," + device.getAddress());
        }
    };

    Handler mHandler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case READ_MESSAGE:
                    readText.setText((String) msg.obj);
                    break;
            }
            return false;
        }
    });

    private class MyBlueAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mBluetoothDeviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = null;

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, null);
                viewHolder = new ViewHolder();
                viewHolder.info = convertView.findViewById(R.id.name);
                viewHolder.connection = convertView.findViewById(R.id.connection);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            String info1 = "已配对";
            if (mBluetoothDeviceList.get(position).getBondState() != BluetoothDevice.BOND_BONDED) {
                //未配对设备
                info1 = "未配对";
            } else {
                //已经配对过的设备
                info1 = "已配对";
            }
            viewHolder.info.setText(mBluetoothDeviceList.get(position).getName() + "\n"
                    + mBluetoothDeviceList.get(position).getAddress() + "\n" + info1);
            viewHolder.connection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //连接
                    connectGatt = mBluetoothDeviceList.get(position);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                        mBluetoothAdapter.stopLeScan(callback);
                        mBluetoothGatt = connectGatt.connectGatt(MainActivity.this, true, mBluetoothGattCallback);
                    }


                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView info;
            Button   connection;
        }
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        close();
    }

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
