package com.hotpot.urspeed.android.util;


import java.text.DecimalFormatSymbols;

public class TimeUtil {

    public static String formatMillis(long millis) {
        int seconds = (int) (millis / 1000);
        int milliseconds = (int) (millis % 1000);
        return  String.format("%01d"+ DecimalFormatSymbols.getInstance().getDecimalSeparator()+"%01d", seconds, milliseconds/100);
    }
}
