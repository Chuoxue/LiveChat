package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.awt.font.TextAttribute;

public class Privacy extends AppCompatActivity {

    private TextView toolbar_waiting;
    private Switch switch_audio;
    private Switch switch_collect;

    private SharedPreferences privacy_sp;

    private SharedPreferences login_sp;

    private String isVoice;
    private boolean isCollect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_privacy);

        privacy_sp = getSharedPreferences("audio", MODE_PRIVATE);
        isVoice = privacy_sp.getString("isVoice", "0");
        isCollect = privacy_sp.getBoolean("isCollect", false);

        ImageButton btn_shift = (ImageButton) findViewById(R.id.btn_shift);
        ImageView menu = (ImageView) findViewById(R.id.menu);

        TextView toolbar_id = (TextView) findViewById(R.id.toolbar_id);
        login_sp = getSharedPreferences("login", MODE_PRIVATE);
        toolbar_id.setText(login_sp.getString("account",null));

        switch_audio = (Switch) findViewById(R.id.switch_audio);
        if(isVoice.equals("1")) switch_audio.setChecked(true);
        else switch_audio.setChecked(false);

        switch_collect = (Switch) findViewById(R.id.switch_collect);
        switch_collect.setChecked(isCollect);




        btn_shift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(Privacy.this, MainActivity.class));
            }
        });

        switch_audio.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String str = "";
                if (isChecked == true) {
                    str = "开启";
                    privacy_sp.edit().putString("isVoice", "1").apply();
                } else {
                    str = "关闭";
                    privacy_sp.edit().putString("isVoice", "0").apply();
                }
                //显示提示框
                Toast.makeText(Privacy.this, str, Toast.LENGTH_SHORT).show();
            }
        });

        switch_collect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                String str = "";
                if (isChecked == true) {
                    str = "开启";
                    privacy_sp.edit().putBoolean("isCollect", true).apply();
                } else {
                    str = "关闭";
                    privacy_sp.edit().putBoolean("isCollect", false).apply();
                }
                //显示提示框
                Toast.makeText(Privacy.this, str, Toast.LENGTH_SHORT).show();
            }
        });

    }



    public void ClickAllowDisplay(View view) {
        startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())), 0);

    }

    public void ClickBack(View view) { Privacy.this.finish(); }

    public void ClickShift(View view) {startActivity(new Intent(Privacy.this, MainActivity.class));}

    class MyOnCheckedChangeListener implements CompoundButton.OnCheckedChangeListener {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            String str = "";
            if (b == true) {
                str = "开启";
            } else {
                str = "关闭";
            }
            //显示提示框
            Toast.makeText(Privacy.this, str, Toast.LENGTH_SHORT).show();
        }
    }

}