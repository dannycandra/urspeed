package com.hotpot.urspeed.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.hotpot.urspeed.android.model.Car;
import com.hotpot.urspeed.android.model.Result;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

public class UrSpeedDBHelper extends OrmLiteSqliteOpenHelper {

    // name of the database file for your application -- change to something
    // appropriate for your app
    private static final String DATABASE_NAME = "urspeed";

    // any time you make changes to your database, you may have to increase the
    // database version
    private static final int DATABASE_VERSION = 1;

    private Context context;

    // the DAO object we use to access the any table
    private Dao<Car, Integer> carDao = null;
    private Dao<Result, Integer> resultDao = null;

    public UrSpeedDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
     }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create the tables that will store
     * your data.
     */
    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            Log.i(UrSpeedDBHelper.class.getName(), "onCreate");
            TableUtils.createTableIfNotExists(connectionSource, Car.class);
            TableUtils.createTableIfNotExists(connectionSource, Result.class);

        } catch (SQLException e) {
            Log.e(UrSpeedDBHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    }

    public Dao<Car, Integer> getCarDao() throws SQLException {
        if (carDao == null) {
            carDao = getDao(Car.class);
        }
        return carDao;
    }

    public Dao<Result, Integer> getResultDao() throws SQLException {
        if (resultDao == null) {
            resultDao = getDao(Result.class);
        }
        return resultDao;
    }
}
