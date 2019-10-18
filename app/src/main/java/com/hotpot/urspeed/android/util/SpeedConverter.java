package com.hotpot.urspeed.android.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hotpot.urspeed.android.SharedPreferenceStrings.SharedPreferenceUtils;

public class SpeedConverter {

    public enum TYPE{
        KMH,MPH;

        public static TYPE toTYPE (String typeString) {
            try {
                return valueOf(typeString);
            } catch (Exception ex) {
                // For error cases
                return KMH;
            }
        }

        public static void setTYPE(Context context, TYPE type) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(SharedPreferenceUtils.SPEED_TYPE, type.toString());
            editor.commit();
        }

        public static TYPE getTYPE(Context context) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String typeString = sp.getString(SharedPreferenceUtils.SPEED_TYPE, TYPE.KMH.toString());
            return TYPE.toTYPE(typeString);
        }
    }

    public static double convertMeterSecondToHourUnits(double locationSpeed, TYPE type){
        double speed = 0;

        if(locationSpeed <= 0){
            return 0;
        }

        switch(type){
            case KMH:
                speed= (locationSpeed*3600)/1000;
                break;
            case MPH:
                speed=(double) (locationSpeed*2.2369);
                break;
        }

        return speed;
    }

    public static double convertMphToKmh(double mph){
        return mph * 1.609344;
    }

    public static double convertKmhToMph(double kmh){
        return kmh * 0.6213712;
    }
}
