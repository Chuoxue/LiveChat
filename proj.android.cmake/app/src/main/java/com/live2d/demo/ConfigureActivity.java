package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class ConfigureActivity extends AppCompatActivity {

    private SeekBar seekbar1;
    private SeekBar seekbar2;
    private SeekBar seekbar3;

    private TextView val1;
    private TextView val2;
    private TextView val3;

    private String v1;
    private String v2;
    private String v3;

    private SharedPreferences login_sp;
    private SharedPreferences config_sp;
    private String account;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);



        login_sp = getSharedPreferences("login",MODE_PRIVATE);
        account = login_sp.getString("account", null);

        config_sp = getSharedPreferences("config",MODE_PRIVATE);

        TextView toolbar_id = (TextView) findViewById(R.id.toolbar_id);
        toolbar_id.setText(account);
        
        ImageButton btn_confirm = findViewById(R.id.confirm);

        seekbar1 = (SeekBar)findViewById(R.id.seekBar1);
        val1 = (TextView) findViewById(R.id.val1);

        seekbar2 = (SeekBar)findViewById(R.id.seekBar2);
        val2 = (TextView) findViewById(R.id.val2);

        seekbar3 = (SeekBar)findViewById(R.id.seekBar3);
        val3 = (TextView) findViewById(R.id.val3);

        seekbar1.setProgress(config_sp.getInt("progress1",0));
        seekbar2.setProgress(config_sp.getInt("progress2",0));

        seekbar3.setProgress(config_sp.getInt("progress3",0));

        val1.setText(config_sp.getInt("progress1",0)+"");
        val2.setText(config_sp.getInt("progress2",0)+"");
        val3.setText(config_sp.getInt("progress3",0)+"");

        seekbar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = (float)seekbar1.getProgress()/10;
                val1.setText(val+"");
                v1 = val+"";
                config_sp.edit().putInt("progress1",seekbar1.getProgress()).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(ConfigureActivity.this,"Start Touching",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(ConfigureActivity.this,"Stop Touching",Toast.LENGTH_SHORT).show();
            }
        });

        seekbar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = (float)seekbar2.getProgress()/10;
                val2.setText(val+"");
                v2 = val+"";
                config_sp.edit().putInt("progress2",seekbar2.getProgress()).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(ConfigureActivity.this,"Start Touching",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(ConfigureActivity.this,"Stop Touching",Toast.LENGTH_SHORT).show();
            }
        });

        seekbar3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float val = (float)seekbar3.getProgress()/10;
                val3.setText(val+"");
                v3 = val+"";
                config_sp.edit().putInt("progress3",seekbar3.getProgress()).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(ConfigureActivity.this,"Start Touching",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
//                Toast.makeText(ConfigureActivity.this,"Stop Touching",Toast.LENGTH_SHORT).show();
            }
        });


        btn_confirm.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        config();
                    }
                }).start();
                startActivity(new Intent(ConfigureActivity.this, ChatActivity1.class));
            }

        });

    }

    public void config() {
        String addr = "http://192.144.215.117:8080/"+account+"/"+"0"+"/"+v3+"/"+"/"+v2+"/"+v1+"/"+"0";
        try {
            URL url = new URL(addr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5 * 1000);
            connection.connect();
            connection.disconnect();
            Log.d("muwu","addr: "+addr);
        } catch (Exception e) {
            Log.d("MUWU", e.getMessage());
            e.printStackTrace();
        }
    }

    public void ClickBack(View view) { ConfigureActivity.this.finish(); }

    public void ClickShift(View view) {startActivity(new Intent(ConfigureActivity.this, MainActivity.class));}
}