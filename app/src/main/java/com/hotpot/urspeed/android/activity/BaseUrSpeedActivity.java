package com.hotpot.urspeed.android.activity;

import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;

import com.hotpot.urspeed.android.database.UrSpeedDBHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

public class BaseUrSpeedActivity extends AppCompatActivity {

    private UrSpeedDBHelper databaseHelper = null;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            OpenHelperManager.releaseHelper();
            databaseHelper = null;
        }
    }

    protected UrSpeedDBHelper getHelper() {
        if (databaseHelper == null) {
            databaseHelper =
                    OpenHelperManager.getHelper(this, UrSpeedDBHelper.class);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            db.execSQL(" PRAGMA foreign_keys = ON ");
        }
        return databaseHelper;
    }

}
