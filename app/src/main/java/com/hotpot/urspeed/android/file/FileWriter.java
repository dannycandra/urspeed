package com.hotpot.urspeed.android.file;

import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {

    public static void writeToExternal(Bitmap bitmap){
        String path = Environment.getExternalStorageDirectory().toString();
        File file = new File(path + "/DCIM/Urspeed", "UrSpeedExample.png");

        File dir = new File(path + "/DCIM/Urspeed/");
        if(!dir.exists()) {
            dir.mkdirs();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
