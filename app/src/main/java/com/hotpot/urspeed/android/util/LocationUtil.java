package com.hotpot.urspeed.android.util;


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class LocationUtil extends Service implements LocationListener {

    public static final int GPS_ACCEPTABLE_LOCK_ACCURACY = 10;
    public static final int GPS_ACCEPTABLE_ACCURACY = 15;
    public static final int OLD_LOCATION_MILIS_LIMIT = 1000;
    private final Context mContext;

    private boolean isGPSEnabled = false;

    private Location location;  // location
    private double latitude;    // latitude
    private double longitude;   // longitude
    private double speed;       // speed
    private float accuracy;
    private long timestamp;

    // The minimum distance to change Updates in meters
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0.0f; // 1 m

    // The minimum time between updates in milliseconds
    private static final int MIN_TIME_BW_UPDATES = 0; // 0 ms

    // Declaring a Location Manager
    protected LocationManager locationManager;

    public LocationUtil(Context context) {
        this.mContext = context;
        location = null;
        latitude = 0;
        longitude = 0;
        speed = 0;
        accuracy = 0;
        timestamp = 0;
    }

    public void startListeningGPS() {
        try {
            locationManager = (LocationManager) mContext
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            if (!isGPSEnabled) {
                // no network provider is enabled
            } else {
                if (isGPSEnabled) {
                    //noinspection ResourceType
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopUsingGPS(){
        if(locationManager != null){
            //noinspection ResourceType
            locationManager.removeUpdates(LocationUtil.this);
        }
    }

    public Location getLocation(){
        return location;
    }

    public double getLatitude(){
        if(location != null){
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude(){
        if(location != null){
            longitude = location.getLongitude();
        }
        return longitude;
    }

    public double getSpeed(){
        if(location != null){
            speed = location.getSpeed();
        }
        return speed;
    }

    public float getAccuracy(){
        if(location != null){
            accuracy = location.getAccuracy();
        }
        return accuracy;
    }

    public long getTimestamp(){
        return timestamp;
    }

    @Override
    public void onLocationChanged(Location location) {
        long currentTimestamp = System.currentTimeMillis();
        Log.i("Message: ","Location changed, " + location.getAccuracy() + " , " + location.getLatitude()+ "," + location.getLongitude()+ "," + location.getSpeed());
        // first lock must have good accuracy
        if(location == null){
            if(location.getAccuracy() <= GPS_ACCEPTABLE_LOCK_ACCURACY ){
                this.location = location;
                this.timestamp =currentTimestamp;
                Log.i("gps info","position acquired, set first geo fix location");
            }
        } else if(location != null)  {
            // only accept good accuracy or just accept any signal once the the last location was longer than OLD_LOCATION_MILIS_LIMIT
            if(location.getAccuracy() <= GPS_ACCEPTABLE_ACCURACY || (currentTimestamp - timestamp)> OLD_LOCATION_MILIS_LIMIT) {
                this.location = location;
                this.timestamp = currentTimestamp;
                Log.i("gps info", "update location");
            }
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
