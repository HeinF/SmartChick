package com.chicken.smartchick.bluetooth;

        import android.os.Bundle;
        import android.os.Handler;
        import android.os.Message;


public class BlueServiceHandler extends Handler {
    private BluetoothConnService connService;
    private boolean btActivtyMsgrSet = false;

    public BlueServiceHandler(BluetoothConnService connService){
        this.connService = connService;
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.arg1 == Constants.BLUE_ACTIVITY_BIND){
            connService.setBtActivityMsgr(msg.replyTo);
            btActivtyMsgrSet = true;
        }
        if (msg.arg1 == Constants.BLUE_SERVICE_CONNECT){
            Bundle bundle = msg.getData();
            connService.connectDevice(bundle.getString("ADDRESS"));
        }
        if (msg.arg1 == Constants.BLUE_WRITE){
            connService.write(Integer.toString(msg.arg2));
        }
    }
}
