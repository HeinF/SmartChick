package com.chicken.smartchick.bluetooth;

        import android.app.Service;
        import android.bluetooth.BluetoothAdapter;
        import android.bluetooth.BluetoothDevice;
        import android.bluetooth.BluetoothSocket;
        import android.content.Intent;
        import android.os.IBinder;
        import android.os.Message;
        import android.os.Messenger;
        import android.os.RemoteException;
        import android.support.annotation.IntDef;
        import android.util.Log;

        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;
        import java.util.UUID;

public class BluetoothConnService extends Service {
    private static final String TAG = BluetoothConnService.class.getSimpleName();

    private BluetoothAdapter btAdapter;
    private BluetoothDevice btDevice;
    private ConnectINGthread connectING;
    private ConnectEDthread connectED;
    private boolean isConnected = false;

    private Messenger btServiceMsgr = new Messenger(new BlueServiceHandler(this));
    private Messenger btActivityMsgr = null;

    public BluetoothConnService() {
    }

    @Override
    public void onCreate() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return btServiceMsgr.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    public void setBtActivityMsgr(Messenger messenger){
        btActivityMsgr = messenger;
    }

    public boolean isConnected(){
        return isConnected;
    }


    public void connectDevice(String address){
        btDevice = btAdapter.getRemoteDevice(address);
        connectING = new ConnectINGthread(btDevice, Constants.uuidString);
        connectING.start();
    }

    public void write(String toWrite){
        connectED.write(toWrite);
    }

    private class ConnectINGthread extends Thread{
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectINGthread( BluetoothDevice device, String uuid){
            Log.d(TAG, "Connecting Thread");
            btDevice = device;
            BluetoothSocket tmp = null;
            try{
                tmp = btDevice.createInsecureRfcommSocketToServiceRecord(UUID.fromString(uuid));
                Log.d(TAG, "Socket created");
            } catch (IOException e){
                Log.d(TAG, "Socket creation failed: "+e.toString());
                Log.d(TAG, "Stopping service due socket creation failed");
            }
            btSocket = tmp;
        }

        @Override
        public void run() {
            super.run();
            Log.d(TAG, "Connecting thread run()");
            btAdapter.cancelDiscovery();
            try {
                btSocket.connect();
                Log.d(TAG, "BT socket connected");
                connectED = new ConnectEDthread(btSocket);
                connectED.start();
                Log.d(TAG, "Connected thread started");

            } catch (IOException e){
                try {
                    Log.d(TAG, "Socket connection failed: "+e.toString());
                    Log.d(TAG, "Stopping service due socket connection failed");
                    btSocket.close();
                    stopSelf();
                } catch (IOException e2){
                    Log.d(TAG, "Socket closing failed, stopping service "+e2.toString());
                    stopSelf();
                }
            }
        }

        public void closeSocket(){
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.d(TAG, "Socket closing failed, stopping service "+e.toString());
                stopSelf();
            }
        }
    }

    private class ConnectEDthread extends Thread{
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectEDthread(BluetoothSocket socket){
            Log.d(TAG, "Connected thread");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
                Log.d(TAG, "Unable to read/write, stopping service");
                stopSelf();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        @Override
        public void run(){
            if(!isConnected){
                Message msg = Message.obtain();
                msg.arg1 = Constants.IS_CONNECTED;
                msg.arg2 = 1;
                try {
                    btActivityMsgr.send(msg);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

            }
            Log.d(TAG, "Connected thread run()");
            byte[] buffer = new byte[256];
            int bytes;

            while(true){ // Loop should be modified later
                try {
                    bytes = inStream.read(buffer);
                } catch (IOException e) {
                    Log.d(TAG, e.toString());
                    Log.d(TAG, "Unable to read/write, stopping service");
                    stopSelf();
                    break;
                }
            }
        }

        public void write(String message){
            byte[] msgBuffer = message.getBytes();
            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, e.toString());
                Log.d(TAG, "Unable to read/write, stopping service");
                stopSelf();
            }
        }

        public void closeStream(){
            try {
                inStream.close();
                outStream.close();
            } catch (IOException e) {
                Log.d(TAG, e.toString());
                Log.d(TAG, "Failed to close streams, service closing");
                stopSelf();
            }
        }
    }
}

