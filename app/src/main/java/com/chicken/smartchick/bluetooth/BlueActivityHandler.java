package com.chicken.smartchick.bluetooth;

        import android.os.Handler;
        import android.os.Message;


public class BlueActivityHandler extends Handler {
    private BlueActivity blueActivity;

    public BlueActivityHandler(BlueActivity blueActivity){
        this.blueActivity = blueActivity;
    }

    @Override
    public void handleMessage(Message msg) {
        if(msg.arg1 == Constants.IS_CONNECTED){
            switch (msg.arg2){
                case 0:
                    blueActivity.enableOperateBtn(false);
                    break;
                case 1:
                    blueActivity.enableOperateBtn(true);
                    break;

            }
        }
    }
}

