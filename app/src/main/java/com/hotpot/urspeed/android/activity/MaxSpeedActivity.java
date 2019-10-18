package com.hotpot.urspeed.android.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.hotpot.urspeed.android.ClientCache;
import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.SharedPreferenceStrings.SharedPreferenceUtils;
import com.hotpot.urspeed.android.database.UrSpeedDBHelper;
import com.hotpot.urspeed.android.model.Car;
import com.hotpot.urspeed.android.util.LocationUtil;
import com.hotpot.urspeed.android.util.SpeedConverter;
import com.hotpot.urspeed.android.util.TaskScheduler;

import java.sql.SQLException;

public class MaxSpeedActivity extends BaseUrSpeedActivity {

    private enum STATE {
        INIT, RUNNING;
    }

    private class LoopTask implements Runnable{
        @Override
        public void run() {
            loop();
        }
    }

    private LocationManager locationManager = null;

    private STATE state;
    private TextView debugInfoTV;
    private TextView currentSpeedTV;
    private TextView vmaxTV;
    private TextView statusTextTV;
    private TextView speedUnitTV;
    private TextView speedUnitMaxTV;

    private TaskScheduler timer = new TaskScheduler();
    private LoopTask task;
    private LocationUtil locationUtil;
    private UrSpeedDBHelper dbHelper;

    private int maxSpeed;

    private Car car;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_max_speed);

        // gps location and listening to updates every 1 miliseconds
        locationUtil = new LocationUtil(this);

        // DB
        dbHelper = new UrSpeedDBHelper(this);

        // init ui
        debugInfoTV = (TextView) findViewById(R.id.debug_info);
        currentSpeedTV = (TextView) findViewById(R.id.current_speed);
        vmaxTV = (TextView) findViewById(R.id.vmax);
        statusTextTV = (TextView) findViewById(R.id.status_text);
        speedUnitTV = (TextView) findViewById(R.id.speed_unit);
        speedUnitMaxTV = (TextView) findViewById(R.id.speed_unit_max);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.activity_record_speed_dialog_gps_question)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                            dialog.cancel();
                        }
                    });
            final AlertDialog alert = builder.create();
            alert.show();
        } else {
            locationUtil.startListeningGPS();
            speedUnitMaxTV.setVisibility(View.GONE);
            setState(STATE.INIT);
            task = new LoopTask();
            timer.scheduleAtFixedRate(task, 1);
        }

        // get car from profile
        if(ClientCache.getCurrentCar() != null){
            car = ClientCache.getCurrentCar();
            maxSpeed = car.getVmax();
        }else{
            maxSpeed = 0;
        }

        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
        // set maxspeed to mph
        if(speedType == SpeedConverter.TYPE.MPH){
            maxSpeed = (int) SpeedConverter.convertKmhToMph(maxSpeed);
        }

        vmaxTV.setText(Integer.toString(maxSpeed));

        // get ui
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);

        // Show the Up button in the action bar.
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause(){
        super.onPause();
        updateVmaxInDB();
    }

    @Override
    protected void onResume(){
        super.onResume();
        updateVmaxInDB();
    }

    @Override
    protected void onStop(){
        super.onStop();
        locationUtil.stopUsingGPS();
        timer.stop(task);
        updateVmaxInDB();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_max_speed, menu);

        MenuItem dayMenuItem = menu.findItem(R.id.action_day);
        MenuItem nightMenuItem = menu.findItem(R.id.action_night);
        dayMenuItem.setVisible(false);
        nightMenuItem.setVisible(true);

        // get default shared pref
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDayMode = pref.getBoolean(SharedPreferenceUtils.DAY_MODE, true);
        if (!isDayMode) {
            dayMenuItem.setVisible(true);
            nightMenuItem.setVisible(false);
            setNightMode();
        }
        else{
            dayMenuItem.setVisible(false);
            nightMenuItem.setVisible(true);
            setDayMode();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_retry:
                // reset state to init
                setState(STATE.INIT);
                break;
            case R.id.action_day:
                setDayMode();
                // Save to shared preferences
                SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(MaxSpeedActivity.this);
                edit.putBoolean(SharedPreferenceUtils.DAY_MODE, true);
                edit.commit();
                invalidateOptionsMenu();
                break;
            case R.id.action_night:
                setNightMode();
                // Save to shared preferences
                SharedPreferences.Editor edit2 = SharedPreferenceUtils.getSharedEditor(MaxSpeedActivity.this);
                edit2.putBoolean(SharedPreferenceUtils.DAY_MODE, false);
                edit2.commit();
                invalidateOptionsMenu();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        locationUtil.stopUsingGPS();
        timer.stop(task);
        updateVmaxInDB();
    }

    private synchronized void loop() {
        switch (state) {
            case INIT:
                initLoop();
                break;
            case RUNNING:
                runningLoop();
                break;
        }
    }

    private synchronized void initLoop() {
        debugInfoTV.setVisibility(View.INVISIBLE);
        currentSpeedTV.setVisibility(View.INVISIBLE);
        vmaxTV.setVisibility(View.INVISIBLE);
        speedUnitTV.setVisibility(View.INVISIBLE);
        speedUnitMaxTV.setVisibility(View.INVISIBLE);

        statusTextTV.setVisibility(View.VISIBLE);
        statusTextTV.setText(R.string.state_init);

        if(locationUtil.getLocation() != null){
            Log.i("state change", "change state to running");
            setState(STATE.RUNNING);
        }
    }

    private synchronized void runningLoop() {
        debugInfoTV.setVisibility(View.VISIBLE);
        currentSpeedTV.setVisibility(View.VISIBLE);
        vmaxTV.setVisibility(View.VISIBLE);
        speedUnitTV.setVisibility(View.VISIBLE);
        speedUnitMaxTV.setVisibility(View.VISIBLE);

        statusTextTV.setVisibility(View.INVISIBLE);
        statusTextTV.setText("");

        int currentSpeed = 0;
        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
        switch(speedType){
            case KMH:
                speedUnitTV.setText(getResources().getText(R.string.speed_unit_kmh));
                speedUnitMaxTV.setText(getResources().getText(R.string.speed_unit_kmh));
                currentSpeed = (int) getCurrentSpeed(SpeedConverter.TYPE.KMH);
                break;
            case MPH:
                speedUnitTV.setText(getResources().getText(R.string.speed_unit_mph));
                speedUnitMaxTV.setText(getResources().getText(R.string.speed_unit_mph));
                currentSpeed = (int) getCurrentSpeed(SpeedConverter.TYPE.MPH);
                break;
        }

        currentSpeedTV.setText(Integer.toString(currentSpeed));

        if( currentSpeed > maxSpeed){
            maxSpeed = currentSpeed;
            vmaxTV.setText(Integer.toString(currentSpeed));
        }
    }


    private synchronized void setState(STATE state){
        this.state = state;
    }

    private double getCurrentSpeed(SpeedConverter.TYPE type){
        double currentSpeed = 0;
        try {
            currentSpeed = SpeedConverter.convertMeterSecondToHourUnits(locationUtil.getSpeed(), type);
            debugInfoTV.setText("acc: " + locationUtil.getAccuracy() + "lat: " + locationUtil.getLatitude() + " long: " + locationUtil.getLongitude() + " speed:" + currentSpeed);
        }catch(Exception e){
            e.printStackTrace();
        }

        return currentSpeed;
    }

    private void updateVmaxInDB(){
        if(car != null){
            try {
                SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
                switch(speedType){
                    case KMH:
                        if(maxSpeed > car.getVmax()) {
                            car.setVmax(maxSpeed);
                            dbHelper.getCarDao().update(car);
                        }
                        break;
                    case MPH:
                        int maxSpeedKmh = (int) SpeedConverter.convertMphToKmh(maxSpeed);
                        if(maxSpeedKmh > car.getVmax()) {
                            car.setVmax(maxSpeedKmh);
                            dbHelper.getCarDao().update(car);
                        }
                        break;
                }
            } catch (SQLException e) {
                Log.i("sql error","failed to update car vmax");
            }
        }
    }

    private void setDayMode(){
        LinearLayout layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        currentSpeedTV.setTextColor(Color.GRAY);
        debugInfoTV.setTextColor(Color.GRAY);
        speedUnitTV.setTextColor(Color.GRAY);
        speedUnitMaxTV.setTextColor(Color.GRAY);
        vmaxTV.setTextColor(Color.GRAY);
        layoutRoot.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setNightMode(){
        LinearLayout layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        currentSpeedTV.setTextColor(Color.WHITE);
        debugInfoTV.setTextColor(Color.WHITE);
        speedUnitTV.setTextColor(Color.WHITE);
        speedUnitMaxTV.setTextColor(Color.WHITE);
        vmaxTV.setTextColor(Color.WHITE);
        layoutRoot.setBackgroundColor(Color.BLACK);
    }
}
