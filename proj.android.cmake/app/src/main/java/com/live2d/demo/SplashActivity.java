package com.live2d.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class SplashActivity extends Activity implements View.OnClickListener{
    private int recLen = 5;
    private TextView skipText;
    Timer timer = new Timer();
    private Handler handler;
    private  Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();
        timer.schedule(task, 1000, 1000);

        handler = new Handler();
        handler.postDelayed(runnable = new Runnable(){
            @Override
            public void run(){
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, 5000);
    }

    private void initView() {
        skipText = findViewById(R.id.skipText);
        skipText.setOnClickListener(this);

    }
    TimerTask task = new TimerTask() {
        @Override
        public void run() {
            runOnUiThread(new Runnable(){
                @Override
                public void run(){
                    recLen--;
                    skipText.setText("跳过 " + recLen);
                    if(recLen<0){
                        timer.cancel();
                        skipText.setVisibility(View.GONE);
                    }
                }
            });
        }
    };
    //点击skip跳过启动页
    @Override
    public void onClick(View view){
        switch(view.getId()){
            case R.id.skipText:
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            if(runnable != null){
                handler.removeCallbacks(runnable);
            }
            break;
            default:
                break;
        }
    }
}