package com.chicken.smartchick.bluetooth;

        import android.app.Activity;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.le.ScanRecord;
        import android.content.BroadcastReceiver;
        import android.content.ComponentName;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.ServiceConnection;
        import android.nfc.Tag;
        import android.os.IBinder;
        import android.os.Message;
        import android.os.Messenger;
        import android.os.RemoteException;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Log;
        import android.view.View;
        import android.widget.Adapter;
        import android.widget.AdapterView;
        import android.widget.ArrayAdapter;
        import android.widget.Button;
        import android.widget.CompoundButton;
        import android.widget.ListView;
        import android.widget.Switch;
        import android.widget.TextView;
        import android.widget.Toast;

        import com.chicken.smartchick.R;

        import java.util.Set;

public class BlueActivity extends AppCompatActivity {

    private static final String TAG = BlueActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter btAdapter;
    private ArrayAdapter<String> newDevicesAdapter;
    private ArrayAdapter<String> pairedDevicesAdapter;
    private Switch btOnOff;
    private ListView newDevicesListView;
    private ListView pairedDevicesListView;
    private boolean bound = false;
    private boolean open = false;
    private Button searchBtn;
    private Button operate;
    private Messenger blueServiceMessenger;
    private Messenger blueActivtyMessenger = new Messenger(new BlueActivityHandler(this));

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Toast.makeText(BlueActivity.this,"Bound!", Toast.LENGTH_LONG).show();
            Log.d(TAG, "onServiceConnected");
            bound = true;
            blueServiceMessenger = new Messenger(binder);
            Message msg = Message.obtain();
            msg.arg1 = Constants.BLUE_ACTIVITY_BIND;
            msg.replyTo = blueActivtyMessenger;
            try {
                blueServiceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "onServiceConnected failed to send message: " + e.toString());
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blue);

        btAdapter = BluetoothAdapter.getDefaultAdapter();



        pairedDevicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        newDevicesAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        pairedDevicesListView = (ListView) findViewById(R.id.listViewBTpaired);
        pairedDevicesListView.setAdapter(pairedDevicesAdapter);
        pairedDevicesListView.setOnItemClickListener(deviceClickListener);

        newDevicesListView = (ListView) findViewById(R.id.listViewBTfound);
        newDevicesListView.setAdapter(newDevicesAdapter);
        newDevicesListView.setOnItemClickListener(deviceClickListener);

        searchBtn = (Button) findViewById(R.id.buttonBTsearch);
        operate = (Button) findViewById(R.id.buttonOperate);
        operate.setEnabled(false);
        btOnOff = (Switch) findViewById(R.id.switchEnableBT);
        if(btAdapter != null){
            btOnOff.setEnabled(true);
            if (btAdapter.isEnabled()){
                btOnOff.setChecked(true);
                getPairedDevices();
            } else {
                btOnOff.setChecked(false);
            }
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(btReceiver, filter);


        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscover();

            }
        });

        btOnOff.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    enableBT();

                } else {
                    if(btAdapter.isEnabled()){
                        btAdapter.disable();
                    }
                }
            }
        });

        operate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int operation;
                if(!open){
                    operation = Constants.DOOR_OPEN;
                    open = true;
                    operate.setText("Close");
                } else {
                    operation = Constants.DOOR_CLOSE;
                    open = false;
                    operate.setText("Open");
                }
                Message msg = Message.obtain();
                msg.arg1 = Constants.BLUE_WRITE;
                msg.arg2 = operation;
                try {
                    blueServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
        });



    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, BluetoothConnService.class);
        bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bound){
            unbindService(serviceConn);
            bound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(btReceiver);
    }

    private void enableBT(){
        Log.d(TAG, "enableBT()");
        if(btAdapter == null){
            Log.d(TAG, "Device does not have bluetooth");
            //Device does not support bluetooth
        }
        if(!btAdapter.isEnabled()){
            Log.d(TAG, "BT not enabled");
            Log.d(TAG, "Trying to enable BT");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void doDiscover(){
        Log.d(TAG, "Starting Discovery");
        if(btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }
        newDevicesAdapter.clear();
        btAdapter.startDiscovery();
    }

    private void getPairedDevices(){
        pairedDevicesAdapter.clear();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                pairedDevicesAdapter.add(device.getName() +"\n"+ device.getAddress());
            }
        }
        else {
            String noDevices = getResources().getString(R.string.blue_no_paired);
            pairedDevicesAdapter.add(noDevices);
        }
    }

    public void enableOperateBtn(boolean bool){
        operate.setEnabled(bool);
    }

    private AdapterView.OnItemClickListener deviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "List item clicked, canceling discovery in preperation for connection");
            btAdapter.cancelDiscovery();
            String text = ((TextView) view).getText().toString();
            if (text.length() >= 17 && btAdapter.isEnabled()){
                String address = text.substring(text.length()-17);
                Message msg = Message.obtain();
                msg.arg1 = Constants.BLUE_SERVICE_CONNECT;
                Bundle bundle = new Bundle();
                bundle.putString("ADDRESS", address);
                msg.setData(bundle);
                Toast.makeText(BlueActivity.this,address, Toast.LENGTH_LONG).show();
                try {
                    blueServiceMessenger.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == Activity.RESULT_OK){
                    getPairedDevices();
                } else {
                    Log.d(TAG, "Failed to enable bluetooth");
                    btOnOff.setChecked(false);
                }
        }
    }

    final BroadcastReceiver btReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(device.getBondState() != BluetoothDevice.BOND_BONDED){
                    String name = device.getName();
                    if(name == null){
                        name = "Name Unreadable";
                    }
                    String address = device.getAddress();
                    newDevicesAdapter.add(name+"\n"+address);
                }
            }
        }
    };
}
