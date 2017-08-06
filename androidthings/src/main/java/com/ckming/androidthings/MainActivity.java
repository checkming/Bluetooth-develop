package com.ckming.androidthings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

/**
 * 完成任务:
 * 设置蓝牙，查找本地配对或可用的设备，连接设备以及在设备之间传输数据。
 */
public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice device;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayList<String> devicesInfoes = new ArrayList<>();
    private ListView listviewDevice;
    private MyReceiver receiver;
    private ArrayAdapter<String> arrayAdapter;
    private static final String TAG = "MainActivity";
    private BluetoothSocket socket;
    private OutputStream outputStream;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取蓝牙适配器
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
            Toast.makeText(this, "支持蓝牙设备!", Toast.LENGTH_SHORT).show();
        }

        listviewDevice = (ListView) findViewById(R.id.listview);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.item, devicesInfoes);
        listviewDevice.setAdapter(arrayAdapter);

        //再调用广播利用意图过滤器进行注册
        receiver = new MyReceiver();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        //给listview设置点击事件,与蓝牙设备进行连接
        listviewDevice.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long id) {
                new Thread() {
                    @Override
                    public void run() {
                        //停止发现设备
                        bluetoothAdapter.cancelDiscovery();
                        //点击条目 获取到设备
                        BluetoothDevice device = devices.get(position);
                        try {
                            socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                            //连接到socket 服务端
                            socket.connect();
                            //连接到socket 服务端后 会获取一个输出流
                            outputStream = socket.getOutputStream();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "已成功连接!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();

            }
        });


    }

    public void open(View view) {
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    public void close(View view) {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
    }

    public void search(View view) {
        if (bluetoothAdapter != null) {
            //开始搜索其它蓝牙设备
            bluetoothAdapter.startDiscovery();
        }
    }

    public void cease(View view) {
        if (bluetoothAdapter != null) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /*
    灯的控制
     */
    public void openright(View view) {
        sendIOLight(1);
    }

    public void closeright(View view) {

    }

    public void manual(View view) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        if(outputStream !=null){
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 定义广播接收者
     */
    private class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_FOUND)) {
                //那么才说明找到了蓝牙设备
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //将找到的蓝牙设备放到集合中
                devices.add(device);
                //获取到设备的名称与mac地址
                String info = device.getName() + "\n" + device.getAddress();
                //再将设备信息放置集合中
                devicesInfoes.add(info);
                arrayAdapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * 向蓝牙传输数值,控制灯泡
     * @param type
     */
    private void sendIOLight(int type) {
        if(outputStream == null) return;
        try {
            byte[] b = new byte[5];
            b[0] = (byte)0x01;//1
            b[1] = (byte) 0x99;//-103
            if (type==1) {
                //开灯
                b[2] = (byte)0x30;//16
                b[3] = (byte)0x30;
            }else if(type == 2){
                //关灯
                b[2] = (byte)0x31;//17
                b[3] = (byte)0x31;
            }else if(type == 3){
                //点动
                b[2] = (byte)0x33;//25
                b[3] = (byte)0x33;
            }
            b[4] = (byte)0x99;//-103
            outputStream.write(b);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
