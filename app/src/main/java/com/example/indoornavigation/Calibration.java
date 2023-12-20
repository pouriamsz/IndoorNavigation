package com.example.indoornavigation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.indoornavigation.dialog.UserDetailsDialogFragment;
import com.example.indoornavigation.interfaces.OnUserUpdateListener;

import java.util.ArrayList;


public class Calibration extends AppCompatActivity implements OnUserUpdateListener {


    private ListView myList;

    private ArrayList<String> userList;
    private ArrayList<String> strideList;
    private ArrayList<String> preferredStepCounterList;

    private SharedPreferences sharedPreference;
    private SharedPreferences.Editor sharedPreferencesEditor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.calibration);

        sharedPreference = getSharedPreferences(Extra.PREFS_NAME, 0);
        sharedPreferencesEditor = sharedPreference.edit();
        sharedPreferencesEditor.apply();

        userList = Extra.getArrayFromSharedPreferences("user_list", sharedPreference);
        strideList = Extra.getArrayFromSharedPreferences("stride_list", sharedPreference);


        myList = findViewById(R.id.userListView);
        refreshListView();


        //long clicking on a menu item
        myList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                userList.remove(position);
                strideList.remove(position);
                refreshListView();
                updatePrefs();
                return true;
            }
        });

        //clicking on a menu item
        myList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Log.d("User_List_Activity", "click position: " + position);

                Bundle mBundle = new Bundle();
                mBundle.putString("user_name", userList.get(position));
                mBundle.putFloat("stride_length", Float.parseFloat(strideList.get(position)));

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(Calibration.this, InitPosition.class);
                        intent.putExtra("stepLength", Double.parseDouble(strideList.get(position)));
                        startActivity(intent);
                        finish();
                        overridePendingTransition(R.anim.slide_in_left,R.anim.slide_out_left);
                    }
                },600);

            }
        });

        ImageButton addUserBtn = findViewById(R.id.addUserBtn);

        addUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserDetailsDialogFragment userDetailsDialog = new UserDetailsDialogFragment();
                userDetailsDialog.setOnUserUpdateListener(Calibration.this);
                userDetailsDialog.setUserName(null);
                userDetailsDialog.setAddingUser(true);
                userDetailsDialog.show(getFragmentManager(), "Calibration");
            }
        });


    }

    private void refreshListView() {
        ArrayAdapter<String> listAdapter = new ArrayAdapter<>(Calibration.this, android.R.layout.simple_list_item_1, userList);
        myList.setAdapter(listAdapter);
    }

    @Override
    public void onUserUpdateListener(Bundle bundle) {
        userList.add(bundle.getString(UserDetailsDialogFragment.USER_TAG));
        strideList.add(bundle.getString(UserDetailsDialogFragment.STRIDE_LENGTH_TAG));
        preferredStepCounterList.add("0");

        refreshListView();
        updatePrefs();
    }

    private void updatePrefs() {
        Extra.addArrayToSharedPreferences("user_list", userList, sharedPreferencesEditor);
        Extra.addArrayToSharedPreferences("stride_list", strideList, sharedPreferencesEditor);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    public void onResume() {
        super.onResume();

        userList = Extra.getArrayFromSharedPreferences("user_list", sharedPreference);
        strideList = Extra.getArrayFromSharedPreferences("stride_list", sharedPreference);
        preferredStepCounterList = Extra.getArrayFromSharedPreferences("preferred_step_counter", sharedPreference);

        refreshListView();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
