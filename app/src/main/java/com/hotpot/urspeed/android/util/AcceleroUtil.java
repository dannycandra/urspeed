package com.hotpot.urspeed.android.util;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;


public class AcceleroUtil extends Service implements SensorEventListener {

    private float movingThreshold = 10f;
    private final Context mContext;

    SensorManager sensorManager = null;

    private float outputX;
    private float outputY;
    private float outputZ;

    public AcceleroUtil(Context context) {
        this.mContext = context;
        sensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
    }

    public AcceleroUtil(Context context, float movingThreshold) {
        this.mContext = context;
        this.movingThreshold = movingThreshold;
        sensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
    }

    public void startListening(){
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopListening(){
        sensorManager.unregisterListener(this);
    }

    public boolean isAccelerated(){
        if((outputX + outputY + outputZ) / 3 > movingThreshold){
            return true;
        }else{
            return false;
        }
    }

    public boolean isXYZAccelerated(){
        if( outputX > movingThreshold || outputY > movingThreshold || outputZ > movingThreshold){
            return true;
        }else{
            return false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        outputX = event.values[0];
        outputY = event.values[1];
        outputZ = event.values[2];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
