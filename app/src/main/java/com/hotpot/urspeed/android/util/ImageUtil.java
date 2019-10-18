package com.hotpot.urspeed.android.util;

import android.content.Context;
import android.graphics.Bitmap;

public class ImageUtil {

    public static final int WIDTH = 500;
    public static final int HEIGHT = 500;

    public static final String TEMPLATE =  "car_default_pic.png";

    public static Bitmap downscaleImage(Bitmap bitmap, Context context){
        if(bitmap != null){
            return Bitmap.createScaledBitmap(bitmap, WIDTH, HEIGHT, true);
        }else {
            return AssetUtil.getBitmapFromAsset(context , TEMPLATE);
        }
    }

    public static Bitmap getDefaultCarBitmap(Context context){
        return AssetUtil.getBitmapFromAsset(context , TEMPLATE);
    }
}
