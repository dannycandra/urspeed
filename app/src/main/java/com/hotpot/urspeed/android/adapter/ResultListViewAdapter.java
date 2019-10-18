package com.hotpot.urspeed.android.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.share.model.SharePhoto;
import com.facebook.share.model.SharePhotoContent;
import com.facebook.share.widget.ShareDialog;
import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.activity.ResultActivity;
import com.hotpot.urspeed.android.generator.ResultImageGenerator;
import com.hotpot.urspeed.android.model.Car;
import com.hotpot.urspeed.android.model.Result;
import com.hotpot.urspeed.android.util.AppChecker;
import com.hotpot.urspeed.android.util.ImageUtil;
import com.hotpot.urspeed.android.util.SpeedConverter;
import com.hotpot.urspeed.android.util.TimeUtil;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ResultListViewAdapter extends BaseAdapter {
    private Activity activity;
    private LayoutInflater inflater;
    private List<Result> resultItems;

    private static HashMap<ImageView,BitmapWorkerTask> workerMap = new HashMap<>();

    public ResultListViewAdapter(Activity activity, List<Result> resultItems) {
        this.activity = activity;
        this.resultItems = resultItems;
    }

    @Override
    public int getCount() {
        return resultItems.size();
    }

    @Override
    public Object getItem(int position) {
        return resultItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (inflater == null)
            inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null)
            convertView = inflater.inflate(R.layout.result_list_row_item, null);

        // getting picturebox data for the row
        final Result currentResult = resultItems.get(position);

        double startSpeed = 0;
        double targetSpeed = 0;

        // set request text
        TextView resultText = (TextView) convertView.findViewById(R.id.result_text);

        final SpeedConverter.TYPE speedType = SpeedConverter.TYPE.getTYPE(activity);
        String unit = "";
        switch(speedType) {
            case KMH:
                startSpeed = currentResult.getStartSpeed();
                targetSpeed = currentResult.getTargetSpeed();
                unit = activity.getResources().getString(R.string.speed_unit_kmh);
                break;
            case MPH:
                startSpeed = SpeedConverter.convertKmhToMph(currentResult.getStartSpeed());
                targetSpeed = SpeedConverter.convertKmhToMph(currentResult.getTargetSpeed());
                unit = activity.getResources().getString(R.string.speed_unit_mph);
                break;
        }

        resultText.setText((int)startSpeed + " " + unit + " - " + (int)targetSpeed + " " + unit  + " : " +
                TimeUtil.formatMillis(currentResult.getTimeInMilis()) + " " + activity.getResources().getString(R.string.seconds_short_unit));

        // set timestamp
        TextView timestampText = (TextView) convertView.findViewById(R.id.timestamp_text);
        String formattedDate = (DateUtils.getRelativeTimeSpanString(currentResult.getDriveDate().getTime(), new Date().getTime(), DateUtils.FORMAT_ABBREV_RELATIVE)).toString();
        timestampText.setText(formattedDate);

        // set car picture
        ImageView userImageView = (ImageView) convertView.findViewById(R.id.car_thumbnail);

        // check if result has an associated car
        if(currentResult.getCar() != null){
            //Bitmap carBitmap = ImageUtil.downscaleImage(currentResult.getCar().getPhotoBitmap(), activity);
            loadBitmap(currentResult.getCar(),userImageView,activity);
            //userImageView.setImageBitmap(carBitmap);
        }else {
            Bitmap carBitmap = ImageUtil.getDefaultCarBitmap(activity);
            userImageView.setImageBitmap(carBitmap);
        }

        ImageButton shareButton = (ImageButton) convertView.findViewById(R.id.share_button);
        final ResultActivity finalActivity = ((ResultActivity)activity);

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap cardBitmap = ResultImageGenerator.generateImage(finalActivity, currentResult, speedType);

                SharePhoto photo = new SharePhoto.Builder()
                        .setBitmap(cardBitmap)
                        .build();
                SharePhotoContent content = new SharePhotoContent.Builder()
                        .addPhoto(photo)
                        .build();

                if(AppChecker.appInstalled(AppChecker.FACEBOOK_APP, activity)){
                    ShareDialog.show(activity, content);
                }else {
                    Toast.makeText(activity,  activity.getResources().getString(R.string.error_msg_facebook_not_installed),Toast.LENGTH_LONG).show();
                }
            }
        });

        return convertView;
    }


    public void loadBitmap(Car car, ImageView imageView, Context context) {
        if(cancelPotentialWork(car,imageView)){
            BitmapWorkerTask task = new BitmapWorkerTask(imageView,context);
            workerMap.put(imageView,task);
            task.execute(car);
        }
    }

    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            return workerMap.get(imageView);
        }
        return null;
    }

    public static boolean cancelPotentialWork(Car car, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

        if (bitmapWorkerTask != null) {
            final Car bitmapData = bitmapWorkerTask.getData();
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData == null || bitmapData != car) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }

    public class BitmapWorkerTask extends AsyncTask<Car, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private Car data = null;
        private Context context;

        public BitmapWorkerTask(ImageView imageView, Context context) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<>(imageView);
            this.context = context;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Car... params) {
            data = params[0];
            return ImageUtil.downscaleImage(data.getPhotoBitmap(), context);
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }

            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask =
                        getBitmapWorkerTask(imageView);
                if (this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }

        public Car getData() {
            return data;
        }
    }
}
