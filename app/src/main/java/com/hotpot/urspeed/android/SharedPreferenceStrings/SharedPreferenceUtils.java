package com.hotpot.urspeed.android.SharedPreferenceStrings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class SharedPreferenceUtils {
    public static String WARNING_PREF = "warning";
    public static String LAST_SELECTED_PROFILE_POS = "lastProfile";
    public static String HINT_START_PREF = "startHint";
    public static String SPEED_TYPE = "SPEED_TYPE";
    public static String DAY_MODE = "dayMode";

    public static SharedPreferences.Editor getSharedEditor(Activity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        Editor editor = preferences.edit();
        return editor;
    }
}
