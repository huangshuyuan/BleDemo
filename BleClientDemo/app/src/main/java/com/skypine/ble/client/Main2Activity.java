package com.skypine.ble.client;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
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

import com.skypine.ble.client.utils.BleChatService;
import com.skypine.ble.client.utils.ReceiveListener;
import com.skypine.ble.client.utils.ReceiverUtil;

import java.util.ArrayList;
import java.util.List;

import static com.skypine.ble.client.utils.BleChatService.getInstance;

/**
 * 封装后使用
 */
@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)

public class Main2Activity extends AppCompatActivity implements View.OnClickListener, ReceiveListener {
    private String TAG = Main2Activity.class.getSimpleName();

    MyBlueAdapter  mMyBlueAdapter;
    ListView       blueList;
    TextView       readText;
    EditText       sendText;
    BleChatService mBleChatService;
    ReceiverUtil   mReceiverUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mReceiverUtil = ReceiverUtil.getInstance(this);
        mReceiverUtil.addListener(this);

        readText = (TextView) findViewById(R.id.read);
        sendText = (EditText) findViewById(R.id.send_edit);
        blueList = (ListView) findViewById(R.id.list);
        mMyBlueAdapter = new MyBlueAdapter();
        blueList.setAdapter(mMyBlueAdapter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mBleChatService = getInstance(this.getApplicationContext());
        }
        findViewById(R.id.scan).setOnClickListener(this);
        findViewById(R.id.connect).setOnClickListener(this);
        findViewById(R.id.send).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mReceiverUtil.removeListener(this);
        mBleChatService.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    List<BluetoothDevice> mBluetoothDeviceList = new ArrayList<>();

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.scan:

                mBleChatService.setmBluetoothDeviceListener(new BleChatService.BluetoothDeviceListener() {
                    @Override
                    public void onReceive(List<BluetoothDevice> data) {
                        mBluetoothDeviceList = data;
                        mMyBlueAdapter.notifyDataSetChanged();
                    }
                });
                mBleChatService.bleScan();
                break;
            case R.id.connect:

                break;
            case R.id.send:
                byte[] sendValue = sendText.getText().toString().trim().getBytes();
                Log.e(TAG, new String(sendValue));
                try {
                    BleChatService.write(sendValue);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

        }
    }

    @Override
    public void onReceive(String data) {
        readText.setText(data);
    }

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
                    BluetoothDevice connectGatt = mBluetoothDeviceList.get(position);
                    BleChatService.connect(connectGatt);


                }
            });
            return convertView;
        }

        class ViewHolder {
            TextView info;
            Button   connection;
        }
    }

}
