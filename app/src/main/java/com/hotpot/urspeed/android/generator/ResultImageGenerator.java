package com.hotpot.urspeed.android.generator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Environment;

import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.image.Rectangle;
import com.hotpot.urspeed.android.model.Result;
import com.hotpot.urspeed.android.util.AssetUtil;
import com.hotpot.urspeed.android.util.BitmapConverter;
import com.hotpot.urspeed.android.util.SpeedConverter;
import com.hotpot.urspeed.android.util.TimeUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;

public class ResultImageGenerator {

    public static final String FONTS_HEMI_HEAD_BD_IT_TTF = "fonts/hemi_head_bd_it.ttf";
    public static final String TEMPLATE =  "Quartett_ohne_Beschriftung_big_amazon.png";

    private enum TextSize{
        MAIN,LABEL,SUB
    }

    public static Bitmap generateImage(Context context, Result result, SpeedConverter.TYPE type){
        // Coordinates
        Point topLeft = new Point(82,242);
        // value coords
        Rectangle carBrandModelRect = new Rectangle(82, 940, 800, 40);

        Rectangle carAccelRect = new Rectangle(114, 1134, 726, 66);
        Rectangle carVmaxRect = new Rectangle(114, 1294, 338, 66);
        Rectangle carPSRect = new Rectangle(510, 1294, 338, 66);
        Rectangle carYearRect = new Rectangle(114, 1448, 338, 66);
        Rectangle carEngineSizeRect = new Rectangle(510, 1448, 338, 66);

        // label coords
        Rectangle carAccelRectLabel = new Rectangle(114, 1090, 726, 66);
        Rectangle carVmaxRectLabel = new Rectangle(114, 1250, 338, 66);
        Rectangle carPSRectLabel = new Rectangle(510, 1250, 338, 66);
        Rectangle carYearRectLabel = new Rectangle(114, 1404, 338, 66);
        Rectangle carEngineSizeRectLabel = new Rectangle(510, 1404, 338, 66);

        // labels
        String accelLocalized = context.getResources().getString(R.string.card_acceleration);
        String vmaxLocalized = context.getResources().getString(R.string.card_vmax);
        String powerLocalized = context.getResources().getString(R.string.card_horse_power);
        String yearLocalized = context.getResources().getString(R.string.card_build_year);
        String enginesizeLocalized = context.getResources().getString(R.string.card_engine_size);

        // draw background template
        Bitmap backgroundBitmap = AssetUtil.getBitmapFromAsset(context,TEMPLATE);
        backgroundBitmap = convertToMutable(backgroundBitmap);

        Canvas canvas = new Canvas(backgroundBitmap);

        // draw labels
        drawCenteredString(canvas,accelLocalized,carAccelRectLabel,context,TextSize.LABEL);
        drawCenteredString(canvas,vmaxLocalized,carVmaxRectLabel,context,TextSize.LABEL);
        drawCenteredString(canvas,powerLocalized,carPSRectLabel,context,TextSize.LABEL);
        drawCenteredString(canvas,yearLocalized,carYearRectLabel,context,TextSize.LABEL);
        drawCenteredString(canvas,enginesizeLocalized,carEngineSizeRectLabel,context,TextSize.LABEL);

        // unit
        String hpLocalized = context.getResources().getString(R.string.horse_power_unit);
        String naLocalized = context.getResources().getString(R.string.not_available);

        String speedUnit = "";

        double vmax = 0;
        double startSpeed = 0;
        double targetSpeed = 0;
        switch(type){
            case KMH:
                speedUnit = context.getResources().getString(R.string.speed_unit_kmh);
                startSpeed = result.getStartSpeed();
                targetSpeed = result.getTargetSpeed();
                vmax = result.getCar().getVmax();
                break;
            case MPH:
                speedUnit = context.getResources().getString(R.string.speed_unit_mph);
                vmax = SpeedConverter.convertKmhToMph(result.getCar().getVmax());
                startSpeed = SpeedConverter.convertKmhToMph(result.getStartSpeed());
                targetSpeed = SpeedConverter.convertKmhToMph(result.getTargetSpeed());
                break;
        }

        if(result.getCar() != null){
            if(result.getCar().getPhotoBitmap() != null){
                Bitmap carBitmap = BitmapConverter.convertByteArrayToBitmap(result.getCar().getImageBytes());
                carBitmap = convertToMutable(carBitmap);
                canvas.drawBitmap(carBitmap, topLeft.x, topLeft.y, null);
            }
            drawCenteredString(canvas,(result.getCar().getBrand() == null ? naLocalized : result.getCar().getBrand()) + " " + result.getCar().getModel() , carBrandModelRect, context, TextSize.MAIN);
            drawCenteredString(canvas,(result.getCar().getVmax() == 0 ? naLocalized : (int)vmax + " " + speedUnit) ,carVmaxRect,context,TextSize.SUB);
            drawCenteredString(canvas,(result.getCar().getHorsePower() == null ? naLocalized : result.getCar().getHorsePower() + " " + hpLocalized) ,carPSRect,context,TextSize.SUB);
            drawCenteredString(canvas,(result.getCar().getProductionYear() == 0 ? naLocalized : result.getCar().getProductionYear()) + "" ,carYearRect,context,TextSize.SUB);
            drawCenteredString(canvas,(result.getCar().getEngineSize() == null ? naLocalized : result.getCar().getEngineSize() + " cc") ,carEngineSizeRect,context,TextSize.SUB);
        }else {
            drawCenteredString(canvas, naLocalized, carBrandModelRect, context, TextSize.MAIN);
            drawCenteredString(canvas, naLocalized,carVmaxRect,context,TextSize.SUB);
            drawCenteredString(canvas, naLocalized,carPSRect,context,TextSize.SUB);
            drawCenteredString(canvas, naLocalized,carYearRect,context,TextSize.SUB);
            drawCenteredString(canvas, naLocalized,carEngineSizeRect,context,TextSize.SUB);
        }

        String secondsLocalized = context.getResources().getString(R.string.seconds_unit);
        drawCenteredString(canvas, (int)startSpeed + " " + speedUnit  + " - " + (int)targetSpeed + " " + speedUnit + " : " + TimeUtil.formatMillis(result.getTimeInMilis()) + " " + secondsLocalized, carAccelRect, context, TextSize.SUB);

//      saveImage(backgroundBitmap);

        return backgroundBitmap;
    }

    private static void saveImage(Bitmap finalBitmap) {

        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/saved_images");
        myDir.mkdirs();
        Random generator = new Random();
        int n = 10000;
        n = generator.nextInt(n);
        String fname = "Image-"+ n +".jpg";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void drawCenteredString(Canvas canvas, String text, Rectangle rect, Context context, TextSize size) {
        // defining the bounds []
        Rect placeholder = new Rect();
        placeholder.set(rect.getX(),rect.getY(),rect.getX()+rect.getWidth(),rect.getY()+rect.getHeight());

        // set font type, color and size
        Typeface tf = Typeface.createFromAsset(context.getAssets(), FONTS_HEMI_HEAD_BD_IT_TTF);
        Paint paint = new Paint();
        paint.setFlags(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(tf);
        paint.setColor(context.getResources().getColor(R.color.result_text_color));
        switch(size){
            case MAIN:
                paint.setTextSize(60f);
                break;
            case LABEL:
                paint.setTextSize(56f);
                break;
            case SUB:
                paint.setTextSize(40f);
                break;
        }

        // Get bounds
        Rect textBounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), textBounds);

        // Determine the X coordinate for the text
        int x = rect.getX() + (rect.getWidth() / 2 ) - (textBounds.right - textBounds.left) / 2;
        // Determine the Y coordinate for the text
        // int y = rect.getY() + ((rect.getHeight() - (textBounds.top - textBounds.bottom)) / 2);
        int y = rect.getY() ;

        // Draw the String
        canvas.drawText(text, x, y, paint);
    }

    /**
     * Converts a immutable bitmap to a mutable bitmap. This operation doesn't allocates
     * more memory that there is already allocated.
     *
     * @param imgIn - Source image. It will be released, and should not be used more
     * @return a copy of imgIn, but muttable.
     */
    public static Bitmap convertToMutable(Bitmap imgIn) {
        try {
            //this is the file going to use temporally to save the bytes.
            // This file will not be a image, it will store the raw image data.
            File file = new File(Environment.getExternalStorageDirectory() + File.separator + "temp.tmp");

            //Open an RandomAccessFile
            //Make sure you have added uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
            //into AndroidManifest.xml file
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");

            // get the width and height of the source bitmap.
            int width = imgIn.getWidth();
            int height = imgIn.getHeight();
            Bitmap.Config type = imgIn.getConfig();

            //Copy the byte to the file
            //Assume source bitmap loaded using options.inPreferredConfig = Config.ARGB_8888;
            FileChannel channel = randomAccessFile.getChannel();
            MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_WRITE, 0, imgIn.getRowBytes()*height);
            imgIn.copyPixelsToBuffer(map);
            //recycle the source bitmap, this will be no longer used.
            imgIn.recycle();
            System.gc();// try to force the bytes from the imgIn to be released

            //Create a new bitmap to load the bitmap again. Probably the memory will be available.
            imgIn = Bitmap.createBitmap(width, height, type);
            map.position(0);
            //load it back from temporary
            imgIn.copyPixelsFromBuffer(map);
            //close the temporary file and channel , then delete that also
            channel.close();
            randomAccessFile.close();

            // delete the temp file
            file.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imgIn;
    }
}
