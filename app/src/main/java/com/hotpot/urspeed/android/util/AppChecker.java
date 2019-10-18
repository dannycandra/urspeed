package com.hotpot.urspeed.android.util;

import android.content.Context;
import android.content.pm.PackageManager;

public class AppChecker {

    public static final String FACEBOOK_APP = "com.facebook.katana";

    public static boolean appInstalled(String uri, Context context) {
        PackageManager pm = context.getPackageManager();
        boolean app_installed;
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            app_installed = true;
        }
        catch (PackageManager.NameNotFoundException e) {
            app_installed = false;
        }
        return app_installed;
    }
}
