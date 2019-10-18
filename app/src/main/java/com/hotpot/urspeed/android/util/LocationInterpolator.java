package com.hotpot.urspeed.android.util;

import android.location.Location;

public class LocationInterpolator {

    private Location preLastLocation;
    private Location lastLocation;
    private long preLastTimestamp;
    private long lastTimestamp;
    private long currentTimestamp;

    public LocationInterpolator(){
        preLastLocation = null;
        lastLocation = null;
    }

    public double getInterpolatedSpeedInHour(Location currentLocation, long currentTimestamp, SpeedConverter.TYPE type){
        this.currentTimestamp = currentTimestamp;

        if(canInterpolate()){
            if(hasToInterpolate()){
                return getInterpolatedSpeedInSecond(type);
            }else {
                preLastLocation = lastLocation;
                lastLocation = currentLocation;
                preLastTimestamp = lastTimestamp;
                lastTimestamp = currentTimestamp;
                return getSpeedFromCurrentLocation(currentLocation,type);
            }
        }else {
            preLastLocation = lastLocation;
            lastLocation = currentLocation;
            preLastTimestamp = lastTimestamp;
            lastTimestamp = currentTimestamp;
            return getSpeedFromCurrentLocation(currentLocation,type);
        }
    }

    private double getInterpolatedSpeedInSecond(SpeedConverter.TYPE type){
        long deltaTime = lastTimestamp - preLastTimestamp;
        double deltaSpeed = lastLocation.getSpeed() - preLastLocation.getSpeed();

        // speed in meter / second , deltaTime in millisecond
        double accel = deltaSpeed / (deltaTime * 1000);
        long timeSinceLastLocation = calculateTimeSinceLastLocation();

        // accel in meter/second , timeSinceLastLocation in millisecond
        double interpolatedSpeed = (accel * (timeSinceLastLocation * 1000));

        return SpeedConverter.convertMeterSecondToHourUnits(lastLocation.getSpeed() + interpolatedSpeed, type);
    }

    /**
     * Calculate elapsed time since last location timestamp
     * @return
     */
    private long calculateTimeSinceLastLocation() {
        return System.currentTimeMillis() - lastTimestamp;
    }

    /**
     * check if location timestamp is equals to last location timestamp<br>
     * if timestamp is equal, then speed must be interpolated<br>
     * @return
     */
    private boolean hasToInterpolate(){
        if(currentTimestamp == lastTimestamp){
            return true;
        }else {
            return false;
        }
    }

    private boolean canInterpolate() {
        if (lastLocation != null && preLastLocation != null && (preLastTimestamp != lastTimestamp)) {
            return true;
        }else{
            return false;
        }
    }

    private double getSpeedFromCurrentLocation(Location location, SpeedConverter.TYPE type) {
        return SpeedConverter.convertMeterSecondToHourUnits(location.getSpeed(), type);
    }
}
