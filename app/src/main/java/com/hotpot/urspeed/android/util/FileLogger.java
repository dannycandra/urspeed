package com.hotpot.urspeed.android.util;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileLogger {

    private static File logFile;

    private static File getLogFile(){
        synchronized(FileLogger.class) {
            if (logFile == null) {
                File logFile = new File("sdcard/urspeed_log.txt");
                if (!logFile.exists()) {
                    try {
                        logFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return logFile;
    }

    public static void appendLog(String text) {
        BufferedWriter buf = null;
        try {
            //BufferedWriter for performance, true to set append to file flag
            buf = new BufferedWriter(new FileWriter(getLogFile(), true));
            buf.append(text);
            buf.newLine();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                buf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
