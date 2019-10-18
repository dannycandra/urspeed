package com.hotpot.urspeed.android.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hotpot.urspeed.android.ClientCache;
import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.SharedPreferenceStrings.SharedPreferenceUtils;
import com.hotpot.urspeed.android.gui.CropImageView;
import com.hotpot.urspeed.android.model.Car;
import com.hotpot.urspeed.android.util.ImagePicker;
import com.hotpot.urspeed.android.util.SpeedConverter;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EditCarProfileActivity extends BaseUrSpeedActivity {
    private static final int PICK_IMAGE_ID = 234; // the number doesn't matter
    private Bitmap currentCarImage = null;
    private final int PHOTO_WIDTH = 800;
    private final int PHOTO_HEIGHT = 600;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.gc();

        setContentView(R.layout.activity_edit_car_profile);

        // get ui
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);

        Intent intent = getIntent();
        if (intent.hasExtra("Editing")) {
            setValues();
        }
        setChooseImageButtonClickListener();

        // Show the Up button in the action bar.
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_edit_car_profile, menu);

        MenuItem deleteMenu = menu.findItem(R.id.action_delete);

        Intent intent = getIntent();
        if (intent.hasExtra("Editing")) {
            deleteMenu.setVisible(true);
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
            case R.id.action_save:
                setSaveProfile();
                break;
            case R.id.action_delete:
                deleteProfile();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setValues() {
        Car car = ClientCache.getCurrentCar();
        TextView brand = (TextView) findViewById(R.id.brand_text);
        TextView model = (TextView) findViewById(R.id.model_text);
        TextView engine = (TextView) findViewById(R.id.engine_size_text);
        TextView horsepower = (TextView) findViewById(R.id.horsePower_text);
        TextView productionYear = (TextView) findViewById(R.id.prod_year_text);
        TextView vmax = (TextView) findViewById(R.id.vmax_text);

        if (car.getPhotoBitmap() != null) {
            CropImageView imageView = (CropImageView) findViewById(R.id.car_image);
            imageView.setImageBitmap(car.getPhotoBitmap());
        }

        brand.setText(car.getBrand());
        model.setText(car.getModel());
        engine.setText(String.valueOf(car.getEngineSize()));
        horsepower.setText(String.valueOf(car.getHorsePower()));
        productionYear.setText(String.valueOf(car.getProductionYear()));

        int maxSpeed = car.getVmax();
        if(maxSpeed <= 0){
            vmax.setVisibility(View.GONE);
        }else {
            String maxSpeedText = "";
            SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(this);
            switch(speedType){
                case KMH:
                    maxSpeedText = maxSpeed == 0 ? "N/A" : String.valueOf(car.getVmax());
                    vmax.setText(getResources().getString(R.string.activity_edit_car_profile_maximum_speed) + ": " + maxSpeedText + " " + getResources().getString(R.string.speed_unit_kmh));
                    break;
                case MPH:
                    maxSpeedText = maxSpeed == 0 ? "N/A" : String.valueOf((int)SpeedConverter.convertKmhToMph(car.getVmax()));
                    vmax.setText(getResources().getString(R.string.activity_edit_car_profile_maximum_speed) + ": " + maxSpeedText + " " + getResources().getString(R.string.speed_unit_mph));
                    break;
            }
        }

        currentCarImage = car.getPhotoBitmap();
    }

    private void setSaveProfile() {
        TextView brand = (TextView) findViewById(R.id.brand_text);
        TextView model = (TextView) findViewById(R.id.model_text);
        TextView engine = (TextView) findViewById(R.id.engine_size_text);
        TextView horsepower = (TextView) findViewById(R.id.horsePower_text);
        TextView productionYear = (TextView) findViewById(R.id.prod_year_text);

        if(brand.getText().toString().length() == 0){
            Toast.makeText(getApplicationContext(), getResources().getText(R.string.aaactivity_edit_car_profile_save_error_missing_mandatory_toast), Toast.LENGTH_LONG).show();
            return;
        }

        String engineSizeValue = engine.getText().toString();
        String hpValue = horsepower.getText().toString();
        int productionYearValue = 0;

        try {
            productionYearValue = Integer.parseInt(productionYear.getText().toString());
        } catch (Exception e) {
            productionYearValue = 0;
        }

        Intent intent = getIntent();
        if (intent.hasExtra("Editing")) {
            Car car = ClientCache.getCurrentCar();
            car.setBrand(brand.getText().toString());
            car.setModel(model.getText().toString());
            car.setEngineSize(engineSizeValue);
            car.setHorsePower(hpValue);
            car.setProductionYear(productionYearValue);
            if (currentCarImage != null) {
                setCarPhoto(car);
            } else {
                car.setImageBytes(null);
            }
            try {
                getHelper().getCarDao().update(car);
                Toast.makeText(getApplicationContext(), getResources().getText(R.string.activity_edit_car_profile_update_profile_toast), Toast.LENGTH_LONG).show();
            } catch (SQLException e) {
                Log.e("sql exception", "can't save car data", e);
            }
        } else {
            Car newCar = new Car();
            newCar.setBrand(brand.getText().toString());
            newCar.setModel(model.getText().toString());
            newCar.setEngineSize(engineSizeValue);
            newCar.setHorsePower(hpValue);
            newCar.setProductionYear(productionYearValue);
            if (currentCarImage != null) {
                setCarPhoto(newCar);
            } else {
                newCar.setImageBytes(null);
            }
            try {
                getHelper().getCarDao().createOrUpdate(newCar);
                Toast.makeText(getApplicationContext(), "Profile saved", Toast.LENGTH_LONG).show();
            } catch (SQLException e) {
                Log.e("sql exception", "can't save car data", e);
            }

            // count saved profile and set the new inserted profile as last selected profile
            List<Car> carList = new ArrayList<Car>();
            // load saved car
            QueryBuilder<Car, Integer> builder = null;
            try {
                builder = getHelper().getCarDao().queryBuilder();
                builder.orderBy("id", true);  // true for ascending, false for descending
                carList = getHelper().getCarDao().query(builder.prepare());  // returns list of ten items
            } catch (SQLException e) {
                Log.e("sql exception", "can't load car data", e);
            }

            int profileCount = carList.size();
            SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(this);
            edit.putInt(SharedPreferenceUtils.LAST_SELECTED_PROFILE_POS, profileCount);
            edit.commit();
        }
        NavUtils.navigateUpFromSameTask(EditCarProfileActivity.this);
    }

    private void setCarPhoto(Car car) {
        CropImageView imageView = (CropImageView) findViewById(R.id.car_image);
        Bitmap croppedBitmap = imageView.getCroppedImage();
        Bitmap resizedBitmap = BITMAP_RESIZER(croppedBitmap, PHOTO_WIDTH, PHOTO_HEIGHT);
        //Bitmap resizedBitmap = getResizedBitmap(croppedBitmap, PHOTO_WIDTH, PHOTO_HEIGHT);
        car.setPhotoBitmapToByteArray(resizedBitmap);
    }

    public Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // CREATE A MATRIX FOR THE MANIPULATION
        Matrix matrix = new Matrix();
        // RESIZE THE BIT MAP
        matrix.postScale(scaleWidth, scaleHeight);

        // "RECREATE" THE NEW BITMAP
        Bitmap resizedBitmap = Bitmap.createBitmap(
                bm, 0, 0, width, height, matrix, false);
        bm.recycle();
        return resizedBitmap;
    }

    public Bitmap BITMAP_RESIZER(Bitmap bitmap,int newWidth,int newHeight) {
        Bitmap scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);

        float ratioX = newWidth / (float) bitmap.getWidth();
        float ratioY = newHeight / (float) bitmap.getHeight();
        float middleX = newWidth / 2.0f;
        float middleY = newHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bitmap, middleX - bitmap.getWidth() / 2, middleY - bitmap.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

        return scaledBitmap;

    }

    private void deleteProfile() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EditCarProfileActivity.this);
        builder.setMessage(R.string.activity_edit_car_profile_delete_confirmation)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                    try {
                        Car car = ClientCache.getCurrentCar();
                        getHelper().getCarDao().delete(car);

                        Toast.makeText(getApplicationContext(), getResources().getText(R.string.activity_edit_car_profile_delete_profile_toast), Toast.LENGTH_LONG).show();
                        ClientCache.setCurrentCar(null);

                        // set last selected profile to post 0 because last profile is deleted
                        SharedPreferences.Editor edit = SharedPreferenceUtils.getSharedEditor(EditCarProfileActivity.this);
                        edit.putInt(SharedPreferenceUtils.LAST_SELECTED_PROFILE_POS, 0);
                        edit.commit();

                        NavUtils.navigateUpFromSameTask(EditCarProfileActivity.this);
                    } catch (SQLException e) {
                        Log.e("sql exception", "can't save car data", e);
                    }
                }
            })
            .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                    dialog.cancel();
                }
            });
        builder.create();
        builder.show();
    }

    private void setChooseImageButtonClickListener() {
        CropImageView changeImageProfile = (CropImageView) findViewById(R.id.car_image);
        changeImageProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialogRemoveOrChangeCarImage();
            }
        });
    }

    private void showDialogRemoveOrChangeCarImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Set a car picture");
        builder.setItems(R.array.set_car_picture, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    Intent chooseImageIntent = ImagePicker.getPickImageIntent(EditCarProfileActivity.this);
                    startActivityForResult(chooseImageIntent, PICK_IMAGE_ID);
                    dialog.dismiss();
                } else if (item == 1) {
                    CropImageView imageView = (CropImageView) findViewById(R.id.car_image);
                    imageView.setImageBitmap(null);
                    currentCarImage = null;
                    dialog.dismiss();
                }
            }
        });
        AppCompatDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PICK_IMAGE_ID:
                Bitmap bitmap = ImagePicker.getImageFromResult(this, resultCode, data);
                if (bitmap != null) {
                    CropImageView imageView = (CropImageView) findViewById(R.id.car_image);
                    imageView.setImageBitmap(bitmap);
                    currentCarImage = bitmap;
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
