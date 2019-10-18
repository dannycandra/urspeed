package com.hotpot.urspeed.android.model;


import android.graphics.Bitmap;

import com.hotpot.urspeed.android.util.BitmapConverter;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "car")
public class Car {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField
    private String brand = new String();

    @DatabaseField
    private String model = new String();

    @DatabaseField
    private String engineSize = new String();;

    @DatabaseField
    private String horsePower = new String();;

    @DatabaseField
    private int productionYear;

    @DatabaseField
    private int vmax;

    @DatabaseField(dataType = DataType.BYTE_ARRAY)
    byte[] imageBytes;

    @DatabaseField
    private String additionalInfos = new String();

    public Car(){

    }

    @Override
    public String toString() {
        StringBuilder tmp = new StringBuilder();
        tmp.append(brand);
        if( !model.isEmpty()){
            tmp.append(" - ");
            tmp.append(model);
            tmp.append(" ");
        }
        if( productionYear != 0) {
            tmp.append("(");
            tmp.append(productionYear);
            tmp.append(")");
        }

        return tmp.toString();            // What to display in the Spinner list.
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEngineSize() {
        return engineSize;
    }

    public void setEngineSize(String engineSize) {
        this.engineSize = engineSize;
    }

    public String getHorsePower() {
        return horsePower;
    }

    public void setHorsePower(String horsePower) {
        this.horsePower = horsePower;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getProductionYear() {
        return productionYear;
    }

    public void setProductionYear(int productionYear) {
        this.productionYear = productionYear;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public String getAdditionalInfos() {
        return additionalInfos;
    }

    public void setAdditionalInfos(String additionalInfos) {
        this.additionalInfos = additionalInfos;
    }

    public int getVmax() {
        return vmax;
    }

    public void setVmax(int vmax) {
        this.vmax = vmax;
    }

    public Bitmap getPhotoBitmap(){
        if( imageBytes != null ) {
            return BitmapConverter.convertByteArrayToBitmap(imageBytes);
        }
        else{
            return null;
        }
    }

    public void setPhotoBitmapToByteArray(Bitmap bitmap){
        if(bitmap != null) {
            setImageBytes(BitmapConverter.convertBitmapToByteArray(bitmap));
        }
    }
}
