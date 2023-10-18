package com.example.indoornavigation;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    String[] destinationList = {"1", "2", "3"};

    // TODO: Use position instead of string
    String selectedDes = "";
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autoCompleteTextView = findViewById(R.id.autoCompleteText);
        arrayAdapter = new ArrayAdapter<>(this, R.layout.destination_list, destinationList);

        autoCompleteTextView.setAdapter(arrayAdapter);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                selectedDes = adapterView.getItemAtPosition(position).toString();
            }
        });

    }

}
