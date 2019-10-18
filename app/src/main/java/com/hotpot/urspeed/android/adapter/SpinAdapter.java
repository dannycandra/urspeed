package com.hotpot.urspeed.android.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class SpinAdapter<T> extends ArrayAdapter<T> {
    private Context context;
    private List<T> values;

    public SpinAdapter(Context context, int textViewResourceId, List<T> values) {
        super(context, textViewResourceId, values);
        this.context = context;
        this.values = values;
    }

    public int getCount() {
        return values.size();
    }

    public T getItem(int position) {
        return values.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView label = new TextView(context);
        label.setTextColor(Color.BLACK);
        label.setGravity(Gravity.CENTER);
        label.setText(values.toArray(new Object[values.size()])[position]
            .toString());
        return label;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView label = new TextView(context);
        label.setTextColor(Color.BLACK);
        label.setGravity(Gravity.CENTER);
        label.setText(values.toArray(new Object[values.size()])[position]
            .toString());

        // Add padding to the TextView, scaled to device
        final float scale = context.getResources().getDisplayMetrics().density;
        int px = (int) (10 * scale + 0.5f);
        label.setPadding(px, px, px, px);

        return label;
    }
}
