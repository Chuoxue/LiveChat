package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class Help extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }

    public void ClickBack(View view) { Help.this.finish(); }

    public void ClickShift(View view) {startActivity(new Intent(Help.this, MainActivity.class));}
}