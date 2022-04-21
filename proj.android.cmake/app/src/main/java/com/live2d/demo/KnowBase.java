package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class KnowBase extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_know_base);

        ImageView gif = (ImageView) findViewById(R.id.gif);
        Glide.with(this).load(R.drawable.ai_jump).into(gif);
    }
}