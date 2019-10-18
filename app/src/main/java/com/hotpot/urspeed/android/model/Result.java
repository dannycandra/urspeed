package com.hotpot.urspeed.android.model;


import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.util.Date;

@DatabaseTable(tableName = "result")
public class Result {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private double startSpeed;

    @DatabaseField
    private double targetSpeed;

    @DatabaseField
    private long timeInMilis;

    @DatabaseField
    private Date driveDate;

    @DatabaseField(canBeNull = true, foreign = true, columnDefinition = "integer references car(id) on delete cascade", foreignAutoRefresh = true)
    private Car car;

    public Result(){

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public double getStartSpeed() {
        return startSpeed;
    }

    public void setStartSpeed(double startSpeed) {
        this.startSpeed = startSpeed;
    }

    public double getTargetSpeed() {
        return targetSpeed;
    }

    public void setTargetSpeed(double targetSpeed) {
        this.targetSpeed = targetSpeed;
    }

    public long getTimeInMilis() {
        return timeInMilis;
    }

    public void setTimeInMilis(long timeInMilis) {
        this.timeInMilis = timeInMilis;
    }

    public Date getDriveDate() {
        return driveDate;
    }

    public void setDriveDate(Date driveDate) {
        this.driveDate = driveDate;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }
}
