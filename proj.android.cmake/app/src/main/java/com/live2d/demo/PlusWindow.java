package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

public class PlusWindow extends AppCompatActivity {
    public SharedPreferences plus_sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plus_window);

        ImageButton btn_more = (ImageButton) findViewById(R.id.btn_more);
        ImageButton btn_logout = (ImageButton) findViewById(R.id.btn_logout);

        plus_sp = getSharedPreferences("login",MODE_PRIVATE);

        btn_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        finish();
        return true;
    }


}