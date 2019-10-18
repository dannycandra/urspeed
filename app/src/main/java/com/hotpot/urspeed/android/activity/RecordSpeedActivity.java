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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.hotpot.urspeed.android.ClientCache;
import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.SharedPreferenceStrings.SharedPreferenceUtils;
import com.hotpot.urspeed.android.database.UrSpeedDBHelper;
import com.hotpot.urspeed.android.model.Car;
import com.hotpot.urspeed.android.model.Result;
import com.hotpot.urspeed.android.util.AcceleroUtil;
import com.hotpot.urspeed.android.util.LocationInterpolator;
import com.hotpot.urspeed.android.util.LocationUtil;
import com.hotpot.urspeed.android.util.SoundPoolPlayer;
import com.hotpot.urspeed.android.util.SpeedConverter;
import com.hotpot.urspeed.android.util.TaskScheduler;
import com.hotpot.urspeed.android.util.TimeUtil;

import java.sql.SQLException;
import java.util.Date;

public class RecordSpeedActivity extends BaseUrSpeedActivity {

    public static final int LOOP_REFRESH_RATE = 100;

    private enum STATE {
        INIT, WAITING, READY, RUNNING, DONE;
    }

    public enum SPEED {
        SPEED_0_100_KMH(0,100), SPEED_0_200_KMH(0,200), SPEED_50_100_KMH(50,100), SPEED_100_150_KMH(100,150), SPEED_100_200_KMH(100,200),
        SPEED_0_60_MPH(0,96.56064), SPEED_0_100_MPH(0,160.934), SPEED_60_100_MPH(96.56064,160.934);

        private double startSpeed;
        private double targetSpeed;

        private SPEED(double startSpeed, double targetSpeed){
            this.startSpeed = startSpeed;
            this.targetSpeed = targetSpeed;
        }

        public double getStartSpeed() {
            return startSpeed;
        }

        public double getTargetSpeed() {
            return targetSpeed;
        }
    }

    private class LoopTask implements Runnable{
        @Override
        public void run() {
            loop();
        }
    }

    private LocationManager locationManager = null;

    private STATE state;
    private SPEED speed;
    private TextView debugInfoTV;
    private TextView debugInfo2TV;
    private TextView currentSpeedTV;
    private TextView stopwatchTV;
    private TextView timeImprovement;
    private TextView statusTextTV;
    private TextView speedUnitTV;
    private TextView timeUnitTV;
    private TextView waitingHintTV;
    public static String speedVar = "speed";

    private long startRunningTime = 0;
    private long endRunningTime = 0;
    private long currentRunTime = 0;
    private double bestTime = 0;

    private TaskScheduler timer = new TaskScheduler();
    private LoopTask task;
    private LocationUtil locationUtil;
    private AcceleroUtil acceleroUtil;
    private boolean startSpeedAchieved;
    private UrSpeedDBHelper dbHelper;
    private SoundPoolPlayer player;
    private LocationInterpolator locationInterpolator;

    private float gpstolerance;
    private float acceltolerance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_speed);

        // get tolerance values from previous intent
        gpstolerance = (float) getIntent().getExtras().get("gpstol");
        acceltolerance = (float) getIntent().getExtras().get("acceltol");

        // gps location and listening to updates
        locationUtil = new LocationUtil(this);

        // motion sensor
        acceleroUtil = new AcceleroUtil(this,acceltolerance);

        // DB
        dbHelper = new UrSpeedDBHelper(this);

        // sound player
        player = new SoundPoolPlayer(this);

        // get selected speed from previous intent
        speed = (SPEED) getIntent().getExtras().get(speedVar);

        locationInterpolator = new LocationInterpolator();

        // init ui
        debugInfoTV = (TextView) findViewById(R.id.debug_info);
        debugInfo2TV = (TextView) findViewById(R.id.debug_info2);
        currentSpeedTV = (TextView) findViewById(R.id.current_speed);
        stopwatchTV = (TextView) findViewById(R.id.stopwatch);
        timeImprovement = (TextView) findViewById(R.id.time_improvement);
        statusTextTV = (TextView) findViewById(R.id.status_text);
        speedUnitTV = (TextView) findViewById(R.id.speed_unit);
        timeUnitTV = (TextView) findViewById(R.id.time_unit);
        waitingHintTV = (TextView) findViewById(R.id.waiting_hint);

        // get default shared pref
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        boolean dontShowWarning = pref.getBoolean(SharedPreferenceUtils.HINT_START_PREF, false);
        if (!dontShowWarning) {
            showWarningDialog();
        }

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
            acceleroUtil.startListening();
            setState(STATE.INIT);
            task = new LoopTask();
            timer.scheduleAtFixedRate(task, LOOP_REFRESH_RATE);
            startSpeedAchieved = false;
        }

        // get ui
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);

        // Show the Up button in the action bar.
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void showWarningDialog() {
        View checkBoxView = View.inflate(this, R.layout.alert_dialog_with_checkbox_start_activity, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save to shared preferences
                SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(RecordSpeedActivity.this);
                edit.putBoolean(SharedPreferenceUtils.HINT_START_PREF, isChecked);
                edit.commit();
            }
        });

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.warning_title);
        builder.setView(checkBoxView);
        builder.setNegativeButton(getResources().getText(R.string.close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create();
        builder.show();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        locationUtil.stopUsingGPS();
        acceleroUtil.stopListening();
        if(player != null){
            player.release();
        }
        timer.stop(task);
    }

    @Override
    public void onBackPressed() {
        locationUtil.stopUsingGPS();
        acceleroUtil.stopListening();
        if (player != null) {
            player.release();
        }
        timer.stop(task);
        super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_record_speed, menu);

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
                SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(RecordSpeedActivity.this);
                edit.putBoolean(SharedPreferenceUtils.DAY_MODE, true);
                edit.commit();
                invalidateOptionsMenu();
                break;
            case R.id.action_night:
                setNightMode();
                // Save to shared preferences
                SharedPreferences.Editor edit2 = SharedPreferenceUtils.getSharedEditor(RecordSpeedActivity.this);
                edit2.putBoolean(SharedPreferenceUtils.DAY_MODE, false);
                edit2.commit();
                invalidateOptionsMenu();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private synchronized void loop() {
        switch (state) {
            case INIT:
                initLoop();
                break;
            case WAITING:
                waitingLoop();
                break;
            case READY:
                readyLoop();
                break;
            case RUNNING:
                runningLoop();
                break;
            case DONE:
                doneLoop();
                break;
        }
    }

    private synchronized void initLoop() {
        debugInfoTV.setVisibility(View.INVISIBLE);
        currentSpeedTV.setVisibility(View.INVISIBLE);
        stopwatchTV.setVisibility(View.INVISIBLE);
        timeImprovement.setVisibility(View.INVISIBLE);
        speedUnitTV.setVisibility(View.INVISIBLE);
        timeUnitTV.setVisibility(View.INVISIBLE);
        waitingHintTV.setVisibility(View.GONE);

        statusTextTV.setVisibility(View.VISIBLE);
        statusTextTV.setText(R.string.state_init);

        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
        switch(speedType){
            case KMH:
                speedUnitTV.setText(getResources().getText(R.string.speed_unit_kmh));
                break;
            case MPH:
                speedUnitTV.setText(getResources().getText(R.string.speed_unit_mph));
                break;
        }

        // initLoop values
        startRunningTime = 0;
        endRunningTime = 0;
        currentRunTime = 0;
        bestTime = 0;
        startSpeedAchieved = false;

        if(locationUtil.getLocation() != null){
            Log.i("state change", "change state to ready");
            setState(STATE.WAITING);
        }
    }

    private synchronized void waitingLoop() {
        debugInfoTV.setVisibility(View.VISIBLE);
        currentSpeedTV.setVisibility(View.VISIBLE);
        stopwatchTV.setVisibility(View.VISIBLE);
//        timeImprovement.setVisibility(View.VISIBLE);
        speedUnitTV.setVisibility(View.VISIBLE);
        timeUnitTV.setVisibility(View.GONE);
        waitingHintTV.setVisibility(View.VISIBLE);
        statusTextTV.setVisibility(View.INVISIBLE);
        stopwatchTV.setText(getResources().getText(R.string.state_waiting));
        stopwatchTV.setTextSize(50);

        String textWaiting = "";
        double startSpeed = 0;
        double currentSpeed = 0;

        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
        switch(speedType) {
            case KMH:
                startSpeed = speed.getStartSpeed();
                textWaiting = String.format(getResources().getString(R.string.state_waiting_info),speed.getStartSpeed() + getResources().getString(R.string.speed_unit_kmh));
                currentSpeed = getCurrentSpeed(SpeedConverter.TYPE.KMH);
                break;
            case MPH:
                startSpeed = SpeedConverter.convertKmhToMph(speed.getStartSpeed());
                textWaiting = String.format(getResources().getString(R.string.state_waiting_info),SpeedConverter.convertKmhToMph(speed.getStartSpeed()) + getResources().getString(R.string.speed_unit_mph));
                currentSpeed = getCurrentSpeed(SpeedConverter.TYPE.MPH);
                break;
        }
        waitingHintTV.setText(textWaiting);

        currentSpeedTV.setText(String.format("%.0f", currentSpeed));
        if(currentSpeed <= startSpeed){
            startSpeedAchieved = true;
            setState(STATE.READY);
        }else{
            startSpeedAchieved = false;
        }
    }

    private synchronized void readyLoop() {
        debugInfoTV.setVisibility(View.VISIBLE);
        currentSpeedTV.setVisibility(View.VISIBLE);
        stopwatchTV.setVisibility(View.VISIBLE);
//        timeImprovement.setVisibility(View.VISIBLE);
        speedUnitTV.setVisibility(View.VISIBLE);
        timeUnitTV.setVisibility(View.GONE);
        waitingHintTV.setVisibility(View.VISIBLE);

        statusTextTV.setVisibility(View.INVISIBLE);
        stopwatchTV.setText(R.string.state_ready);
        stopwatchTV.setTextSize(50);

        String textWaiting = "";
        double startSpeed = 0;
        double currentSpeed = 0;

        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);

        switch(speed) {
            case SPEED_0_100_KMH:
            case SPEED_0_200_KMH:
            case SPEED_0_60_MPH:
            case SPEED_0_100_MPH:
                switch(speedType) {
                    case KMH:
                        startSpeed = speed.getStartSpeed();
                        currentSpeed = locationInterpolator.getInterpolatedSpeedInHour(locationUtil.getLocation(), locationUtil.getTimestamp(), SpeedConverter.TYPE.KMH);
                        textWaiting = String.format(getResources().getString(R.string.state_ready_info_0));
                        getCurrentSpeed(SpeedConverter.TYPE.KMH); // just showing the debuginfo
                        break;
                    case MPH:
                        startSpeed = SpeedConverter.convertKmhToMph(speed.getStartSpeed());
                        currentSpeed = locationInterpolator.getInterpolatedSpeedInHour(locationUtil.getLocation(), locationUtil.getTimestamp(), SpeedConverter.TYPE.MPH);
                        textWaiting = String.format(getResources().getString(R.string.state_ready_info_0));
                        getCurrentSpeed(SpeedConverter.TYPE.MPH); // just showing the debuginfo
                        break;
                }
                waitingHintTV.setText(textWaiting);
                if ((startSpeedAchieved) && ((currentSpeed >= startSpeed+ gpstolerance) || (acceleroUtil.isXYZAccelerated()))){
                    waitingHintTV.setVisibility(View.GONE);
                    debugInfo2TV.setText(debugInfo2TV.getText().toString() + ": ss=" + startSpeedAchieved + ",currentSpeed="+currentSpeed+",xyz="+acceleroUtil.isXYZAccelerated());
                    setState(STATE.RUNNING);
                    Log.i("urspeed state", "change state to runningLoop. CurrentSpeed:" + currentSpeed);
                    Log.i("speed info", "currentSpeed:" + currentSpeed + "triggeringSpeed:" + startSpeed);
                    startRunningTime = System.currentTimeMillis();
                    player.playBeep();
                }
                break;
            case SPEED_50_100_KMH:
            case SPEED_100_150_KMH:
            case SPEED_100_200_KMH:
            case SPEED_60_100_MPH:
                switch(speedType) {
                    case KMH:
                        startSpeed = speed.getStartSpeed();
                        currentSpeed = locationInterpolator.getInterpolatedSpeedInHour(locationUtil.getLocation(), locationUtil.getTimestamp(), SpeedConverter.TYPE.KMH);
                        textWaiting = String.format(getResources().getString(R.string.state_ready_info), (int) startSpeed + " " + getResources().getString(R.string.speed_unit_kmh));
                        getCurrentSpeed(SpeedConverter.TYPE.KMH); // just showing the debuginfo
                        break;
                    case MPH:
                        startSpeed = SpeedConverter.convertKmhToMph(speed.getStartSpeed());
                        currentSpeed = locationInterpolator.getInterpolatedSpeedInHour(locationUtil.getLocation(), locationUtil.getTimestamp(), SpeedConverter.TYPE.MPH);
                        textWaiting = String.format(getResources().getString(R.string.state_ready_info), (int) startSpeed + " " + getResources().getString(R.string.speed_unit_mph));
                        getCurrentSpeed(SpeedConverter.TYPE.MPH); // just showing the debuginfo
                        break;
                }
                waitingHintTV.setText(textWaiting);
                if ((startSpeedAchieved) && ((currentSpeed >= startSpeed+ gpstolerance))){
                    waitingHintTV.setVisibility(View.GONE);
                    debugInfo2TV.setText(debugInfo2TV.getText().toString() + ": ss=" + startSpeedAchieved + ",currentSpeed="+currentSpeed+",xyz="+acceleroUtil.isXYZAccelerated());
                    setState(STATE.RUNNING);
                    Log.i("urspeed state", "change state to runningLoop. CurrentSpeed:" + currentSpeed);
                    Log.i("speed info", "currentSpeed:" + currentSpeed + "triggeringSpeed:" + startSpeed);
                    startRunningTime = System.currentTimeMillis();
                    player.playBeep();
                }
                break;
        }

        currentSpeedTV.setText(String.format("%.0f", currentSpeed));
    }

    private synchronized void runningLoop() {
        debugInfoTV.setVisibility(View.VISIBLE);
        currentSpeedTV.setVisibility(View.VISIBLE);
        stopwatchTV.setVisibility(View.VISIBLE);
        stopwatchTV.setTextSize(90);
//        timeImprovement.setVisibility(View.VISIBLE);
        speedUnitTV.setVisibility(View.VISIBLE);
        timeUnitTV.setVisibility(View.VISIBLE);
        waitingHintTV.setVisibility(View.GONE);

        statusTextTV.setVisibility(View.INVISIBLE);
        statusTextTV.setText("");

        double startSpeed = 0;
        double targetSpeed = 0;
        double currentSpeed = 0;

        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
        switch(speedType) {
            case KMH:
                startSpeed = speed.getStartSpeed();
                targetSpeed = speed.getTargetSpeed();
                currentSpeed = locationInterpolator.getInterpolatedSpeedInHour(locationUtil.getLocation(), locationUtil.getTimestamp(), SpeedConverter.TYPE.KMH);
                break;
            case MPH:
                startSpeed = SpeedConverter.convertKmhToMph(speed.getStartSpeed());
                targetSpeed = SpeedConverter.convertKmhToMph(speed.getTargetSpeed());
                currentSpeed = locationInterpolator.getInterpolatedSpeedInHour(locationUtil.getLocation(), locationUtil.getTimestamp(), SpeedConverter.TYPE.MPH);
                break;
        }

        // Target speed reached, run is done
        if(currentSpeed >= targetSpeed){
            endRunningTime = System.currentTimeMillis();
            currentRunTime = endRunningTime-startRunningTime;

            currentSpeedTV.setText(String.format("%.0f", targetSpeed));
            stopwatchTV.setText(TimeUtil.formatMillis(currentRunTime));

            if(currentRunTime < bestTime){
                bestTime = currentRunTime;
            }

            // save in db
            Result result = new Result();
            result.setStartSpeed(startSpeed);
            result.setTargetSpeed(targetSpeed);
            result.setDriveDate(new Date());
            result.setTimeInMilis(currentRunTime);

            // get selected car
            Car car = ClientCache.getCurrentCar();

            // set car data
            if(car != null){
                result.setCar(car);
            }

            try {
                dbHelper.getResultDao().create(result);
            } catch (SQLException e) {
                Log.i("sql error","failed to create new result");
            }

            setState(STATE.DONE);
            Log.i("speed info", "currentSpeed:" + currentSpeed + "triggeringSpeed:" + startSpeed);
            Log.i("urspeed state", "change state to doneLoop");
            player.playDoubleBeep();
        }else{
            // updating speed and timer
            currentSpeedTV.setText(String.format("%.0f", currentSpeed));
            stopwatchTV.setText(TimeUtil.formatMillis(System.currentTimeMillis() - startRunningTime));
        }
    }

    private synchronized void doneLoop() {
        // not used anymore
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

    private void setDayMode(){
        LinearLayout layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        currentSpeedTV.setTextColor(Color.GRAY);
        stopwatchTV.setTextColor(Color.GRAY);
        debugInfoTV.setTextColor(Color.GRAY);
        debugInfo2TV.setTextColor(Color.GRAY);
        speedUnitTV.setTextColor(Color.GRAY);
        timeUnitTV.setTextColor(Color.GRAY);
        waitingHintTV.setTextColor(Color.GRAY);
        timeImprovement.setTextColor(Color.GRAY);
        layoutRoot.setBackgroundColor(Color.TRANSPARENT);
    }

    private void setNightMode(){
        LinearLayout layoutRoot = (LinearLayout) findViewById(R.id.layout_root);
        currentSpeedTV.setTextColor(Color.WHITE);
        stopwatchTV.setTextColor(Color.WHITE);
        debugInfoTV.setTextColor(Color.WHITE);
        debugInfo2TV.setTextColor(Color.WHITE);
        speedUnitTV.setTextColor(Color.WHITE);
        timeUnitTV.setTextColor(Color.WHITE);
        waitingHintTV.setTextColor(Color.WHITE);
        timeImprovement.setTextColor(Color.WHITE);
        layoutRoot.setBackgroundColor(Color.BLACK);
    }
}
