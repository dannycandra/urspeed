package com.hotpot.urspeed.android.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.hotpot.urspeed.android.ClientCache;
import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.SharedPreferenceStrings.SharedPreferenceUtils;
import com.hotpot.urspeed.android.adapter.SpinAdapter;
import com.hotpot.urspeed.android.model.Car;
import com.hotpot.urspeed.android.model.Result;
import com.hotpot.urspeed.android.util.SoundPoolPlayer;
import com.hotpot.urspeed.android.util.SpeedConverter;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class MainActivity extends BaseUrSpeedActivity {
    private final int DEFAULT_CAR_INDEX = 0;
    private ArrayAdapter<Car> profileAdapter;
    private List<Car> carList = new ArrayList<Car>();
    private Car defaultCar = new Car();
    private RecordSpeedActivity.SPEED selectedSpeed;
    private boolean vmaxMeasurement = false;
    private SoundPoolPlayer player;
    private ToggleButton toggleUnitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        defaultCar.setBrand(getResources().getString(R.string.activity_main_default_brand));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set Admob
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        player = new SoundPoolPlayer(this);

        // speed spinner
        final Spinner speedSpinner = (Spinner) findViewById(R.id.speed_spinner);

        final ImageButton unitButton = (ImageButton) findViewById(R.id.unit_button);

        // get speed from resource
        String[] speedArray = getResources().getStringArray(R.array.speeds_array_kmh);
        SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
        switch(speedType){
            case KMH:
                speedArray = getResources().getStringArray(R.array.speeds_array_kmh);
                break;
            case MPH:
                speedArray = getResources().getStringArray(R.array.speeds_array_mph);
                break;
        }

        List<String> speedList = new ArrayList<String>();
        for( String speed : speedArray ){
            speedList.add(speed);
        }

        unitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get speed from resource
                String[] speedArray = getResources().getStringArray(R.array.speeds_array_kmh);
                SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(MainActivity.this);
                switch(speedType){
                    case KMH:
                        speedArray = getResources().getStringArray(R.array.speeds_array_mph);
                        SpeedConverter.TYPE.setTYPE(MainActivity.this, SpeedConverter.TYPE.MPH);
                        break;
                    case MPH:
                        speedArray = getResources().getStringArray(R.array.speeds_array_kmh);
                        SpeedConverter.TYPE.setTYPE(MainActivity.this, SpeedConverter.TYPE.KMH);
                        break;
                }

                final List<String> speedList = new ArrayList<String>();
                for( String speed : speedArray ){
                    speedList.add(speed);
                }

                ArrayAdapter<String> speedAdapter = new SpinAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, speedList);
                speedSpinner.setAdapter(speedAdapter);
            }
        });

        ArrayAdapter<String> speedAdapter = new SpinAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, speedList);
        speedSpinner.setAdapter(speedAdapter);

        final Context thisContext = this;
        speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(thisContext);
                switch(speedType){
                    case KMH:
                        switch (position) {
                            case 0:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_0_100_KMH;
                                vmaxMeasurement = false;
                                break;
                            case 1:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_0_200_KMH;
                                vmaxMeasurement = false;
                                break;
                            case 2:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_50_100_KMH;
                                vmaxMeasurement = false;
                                break;
                            case 3:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_100_150_KMH;
                                vmaxMeasurement = false;
                                break;
                            case 4:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_100_200_KMH;
                                vmaxMeasurement = false;
                                break;
                            case 5:
                                vmaxMeasurement = true;
                                break;
                        }
                        break;
                    case MPH:
                        switch (position) {
                            case 0:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_0_60_MPH;
                                vmaxMeasurement = false;
                                break;
                            case 1:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_0_100_MPH;
                                vmaxMeasurement = false;
                                break;
                            case 2:
                                selectedSpeed = RecordSpeedActivity.SPEED.SPEED_60_100_MPH;
                                vmaxMeasurement = false;
                                break;
                            case 3:
                                vmaxMeasurement = true;
                                break;
                        }
                        break;
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Callback registration
        setButtonResultClickListener();
        setButtonCreateProfileClickListener();
        setButtonEditProfileClickListener();
        setStartButtonClickListener();
        setMockButtonClickListener();

        // load saved car
        QueryBuilder<Car, Integer> builder = null;
        try {
            builder = getHelper().getCarDao().queryBuilder();
            builder.orderBy("id", true);  // true for ascending, false for descending
            carList = getHelper().getCarDao().query(builder.prepare());  // returns list of ten items
        } catch (SQLException e) {
            Log.e("sql exception", "can't load car data", e);
        }
        // load saved results

        // add default car
        carList.add(DEFAULT_CAR_INDEX, defaultCar);

        // get default shared pref
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(MainActivity.this);

        // profile spinner
        Spinner profileSpinner = (Spinner) findViewById(R.id.profile_spinner);
        profileAdapter = new SpinAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, carList);
        profileSpinner.setAdapter(profileAdapter);
        profileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != DEFAULT_CAR_INDEX) {
                    Car selectedCar = profileAdapter.getItem(position);
                    ClientCache.setCurrentCar(selectedCar);

                    // save last selected profile
                    SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(MainActivity.this);
                    edit.putInt(SharedPreferenceUtils.LAST_SELECTED_PROFILE_POS, position);
                    edit.commit();
                } else {
                    ClientCache.setCurrentCar(null);
                }
                // setEditButtonVisibility();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        int lastProfilePos = pref.getInt(SharedPreferenceUtils.LAST_SELECTED_PROFILE_POS, 0);
        if (lastProfilePos != 0) {
            profileSpinner.setSelection(lastProfilePos);
        }

        boolean dontShowWarning = pref.getBoolean(SharedPreferenceUtils.WARNING_PREF, false);
        if (!dontShowWarning) {
            showWarningDialog();
        }
    }

    private void showWarningDialog() {
        View checkBoxView = View.inflate(this, R.layout.alert_dialog_with_checkbox_main_activity, null);
        CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save to shared preferences
                SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(MainActivity.this);
                edit.putBoolean(SharedPreferenceUtils.WARNING_PREF, isChecked);
                edit.commit();
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    private void setStartButtonClickListener() {
        Button startBtn = (Button) findViewById(R.id.start);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText gpsTol = (EditText) findViewById(R.id.editTextGPSTol);
                EditText accelTol = (EditText) findViewById(R.id.editTextAccelTol);

                float gpsTolFloat = Float.parseFloat(gpsTol.getText().toString());
                float accelTolFloat = Float.parseFloat(accelTol.getText().toString());

                if (vmaxMeasurement) {
                    Intent recordIntent = new Intent(MainActivity.this, MaxSpeedActivity.class);
                    startActivity(recordIntent);
                } else {
                    Intent recordIntent = new Intent(MainActivity.this, RecordSpeedActivity.class);
                    recordIntent.putExtra("gpstol", gpsTolFloat);
                    recordIntent.putExtra("acceltol", accelTolFloat);

                    switch (selectedSpeed) {
                        case SPEED_0_100_KMH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_0_100_KMH);
                            break;
                        case SPEED_50_100_KMH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_50_100_KMH);
                            break;
                        case SPEED_100_150_KMH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_100_150_KMH);
                            break;
                        case SPEED_100_200_KMH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_100_200_KMH);
                            break;
                        case SPEED_0_60_MPH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_0_60_MPH);
                            break;
                        case SPEED_0_100_MPH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_0_100_MPH);
                            break;
                        case SPEED_60_100_MPH:
                            recordIntent.putExtra(RecordSpeedActivity.speedVar, RecordSpeedActivity.SPEED.SPEED_60_100_MPH);
                            break;

                    }
                    startActivity(recordIntent);
                }

            }
        });
    }

    private void setButtonResultClickListener() {
        Button resultBtn = (Button) findViewById(R.id.result);
        resultBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Starting Main Activity
                Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setMockButtonClickListener() {
        Button mockBtn = (Button) findViewById(R.id.mock);
        mockBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                player.playBeep();
                mockDummyResultData();
            }
        });
    }

    private void mockDummyResultData() {
        Car currentCar = ClientCache.getCurrentCar();
        if (currentCar == null) {
            Toast.makeText(getApplicationContext(), "No Profile selected", Toast.LENGTH_LONG).show();
            return;
        }

        Result result = new Result();
        result.setCar(ClientCache.getCurrentCar());
        result.setTimeInMilis(5300);
        result.setDriveDate(new Date());
        result.setTargetSpeed(100);
        result.setStartSpeed(0);

        try {
            getHelper().getResultDao().create(result);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setButtonCreateProfileClickListener() {
        ImageButton createProfileBtn = (ImageButton) findViewById(R.id.create_profile);
        createProfileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EditCarProfileActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setButtonEditProfileClickListener() {
        ImageButton editProfileButton = (ImageButton) findViewById(R.id.edit_profile);
        editProfileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ClientCache.getCurrentCar() == null) {
                    Toast.makeText(getApplicationContext(), getResources().getText(R.string.activity_main_error_msg_no_profile_selected), Toast.LENGTH_LONG).show();
                } else {
                    Intent intent = new Intent(MainActivity.this, EditCarProfileActivity.class);
                    intent.putExtra("Editing", true);
                    startActivity(intent);
                }
            }
        });
    }

    private void setEditButtonVisibility() {
        ImageButton editProfileButton = (ImageButton) findViewById(R.id.edit_profile);
        Car car = ClientCache.getCurrentCar();
        if (ClientCache.getCurrentCar() != null) {
            editProfileButton.setVisibility(View.VISIBLE);
        } else {
            editProfileButton.setVisibility(View.INVISIBLE);
        }
    }
}
