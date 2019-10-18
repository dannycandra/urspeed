package com.hotpot.urspeed.android.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.hotpot.urspeed.android.R;
import com.hotpot.urspeed.android.adapter.ResultListViewAdapter;
import com.hotpot.urspeed.android.model.Result;
import com.j256.ormlite.stmt.QueryBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends BaseUrSpeedActivity {
    private SwipeMenuListView listView;

    private List<Result> list = new ArrayList<>();
    private ResultListViewAdapter adapter;

    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        loadResult();

        initializeListView();

        // get ui
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);

        // Show the Up button in the action bar.
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_result, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_clear_all:
                // reset state to init
                setClearRecord();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void loadResult() {
        // load saved car
        QueryBuilder<Result, Integer> builder = null;
        try {
            list.clear();
            builder = getHelper().getResultDao().queryBuilder();
            builder.orderBy("id", true);  // true for ascending, false for descending
            list = getHelper().getResultDao().query(builder.prepare());  // returns list of ten items
        } catch (SQLException e) {
            Log.e("sql exception", "can't load car data", e);
        }
    }

    private void setClearRecord() {
        AlertDialog.Builder builder = new AlertDialog.Builder(ResultActivity.this);
        builder.setMessage(R.string.activity_result_clear_records_confirmation)
            .setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                    try {
                        getHelper().getResultDao().delete(list);
                        list.clear();
                        adapter.notifyDataSetChanged();
                        Toast.makeText(getApplicationContext(), getResources().getText(R.string.activity_result_clear_records), Toast.LENGTH_SHORT).show();
                    } catch (SQLException e) {
                        Log.e("sql exception", "can't clear records", e);
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

    private void initializeListView() {
        // set adapter
        listView = (SwipeMenuListView) findViewById(R.id.list_view);
        adapter = new ResultListViewAdapter(this, list);
        listView.setAdapter(adapter);

        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                // create "open" item
                SwipeMenuItem openItem = new SwipeMenuItem(getApplicationContext());
                // create "delete" item
                SwipeMenuItem deleteItem = new SwipeMenuItem(getApplicationContext());
                // set item background
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9, 0x3F, 0x25)));
                deleteItem.setWidth(200);
                deleteItem.setTitle(getResources().getString(R.string.activity_edit_car_profile_delete));
                deleteItem.setTitleSize(18);
                deleteItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(deleteItem);
            }
        };

        // set creator
        listView.setMenuCreator(creator);

        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                Result result = (Result)adapter.getItem(position);
                switch (index){
                    case 0:
                        try {
                            getHelper().getResultDao().delete(result);
                            list.remove(result);
                            adapter.notifyDataSetChanged();
                        } catch (SQLException e) {
                            Log.e("sql exception", "can't clear records", e);
                        }
                        break;
                }
                return false;
            }
        });
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
