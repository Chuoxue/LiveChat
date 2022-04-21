package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class LoginActivity extends AppCompatActivity {
    private String account = "";
    private String password = "";
    public SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        try{
            //需要的权限
            String[] permArr={
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.RECORD_AUDIO,
            };
            boolean needReq = false;
            for(int i=0;i<permArr.length;i++){
                if(ContextCompat.checkSelfPermission(this, permArr[i])!= PackageManager.PERMISSION_GRANTED){
                    needReq = true;
                    break;
                }
            }
            if(needReq)
                ActivityCompat.requestPermissions(this,permArr,1);
        }catch (Exception e) {
            LogUtil.w("动态申请权限时，发生异常。", e);
        }

        ImageButton btn_login = (ImageButton) findViewById(R.id.btn_login);
        ImageButton btn_register = (ImageButton) findViewById(R.id.btn_register);

        sp = getSharedPreferences("login", MODE_PRIVATE);


        if(sp.getBoolean("logged", false)){
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        btn_login.setOnClickListener(View->{
            account = ((EditText)findViewById(R.id.et_user_name)).getText().toString();
            password = ((EditText)findViewById(R.id.et_psw)).getText().toString();


            final String[] msg = {""};
            new Thread() {
                @Override
                public void run() {
                    msg[0] = loginByHttpGet(account, password);
                    notice("登录成功", "登录失败");
                }
            }.start();
        });

        btn_register.setOnClickListener(View->{
            account = ((EditText)findViewById(R.id.et_user_name)).getText().toString();
            password = ((EditText)findViewById(R.id.et_psw)).getText().toString();

            final String[] msg = {""};
            Thread t1 = new Thread() {
                @Override
                public void run() {
                    msg[0] = registerByHttpGet(account, password);
                    notice("注册成功", "注册失败");
                }
            };
            t1.start();
            try {
                t1.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Toast.makeText(this,"注册成功",Toast.LENGTH_SHORT).show();
        });
    }

    public String loginByHttpGet(String account, String password) {
        String message = "";
        String addr = "http://192.144.215.117:8080/login/"+account+"/"+password;
        try {
            URL url = new URL(addr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5 * 1000);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            byte[] data = new byte[1024];
            StringBuffer sb = new StringBuffer();
            int length = 0;
            while ((length = inputStream.read(data)) != -1) {
                String s = new String(data, Charset.forName("utf-8"));
                sb.append(s);
            }
            message = sb.toString();
            inputStream.close();
            connection.disconnect();
            if(message.substring(0,1).equals("1")) {
                Log.d("muwu", "登陆成功");
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                saveState(sp, account);
                finish();
            }else {
                Log.d("muwu", "登陆失败");
            }
        } catch (Exception e) {
            Log.d("MUWU", e.getMessage());
            e.printStackTrace();
        }
        return message;
    }

    public String registerByHttpGet(String account, String password) {
        String message = "";
        String addr = "http://192.144.215.117:8080/register/"+account+"/"+password;
        try {
            URL url = new URL(addr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5 * 1000);
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            byte[] data = new byte[1024];
            StringBuffer sb = new StringBuffer();
            int length = 0;
            while ((length = inputStream.read(data)) != -1) {
                String s = new String(data, Charset.forName("utf-8"));
                sb.append(s);
            }
            message = sb.toString();
            inputStream.close();
            connection.disconnect();
            if(message.substring(0,1).equals("1")) {
                Log.d("muwu", "注册成功"+message);
                Intent intent = new Intent(this, ConfigureActivity.class);
                startActivity(intent);
                saveState(sp, account);
                finish();
            }else {
                Log.d("muwu", "注册失败"+message);
            }
        } catch (Exception e) {
            Log.d("MUWU", e.getMessage());
            e.printStackTrace();
        }
        return message;
    }



    private void saveState(SharedPreferences sp, String account){
        sp.edit().putBoolean("logged", true).apply();
        sp.edit().putString("account", account).apply();
    }

    private void notice(String success, String fail) {
        Toast toast = null;
        try {
            if(toast!=null){
                if(sp.getBoolean("logged", false)) {
                    toast.setText(success);
                }
                else toast.setText(fail);

            } else {
                if(sp.getBoolean("logged", false)) {
                    toast = Toast.makeText(this,success,Toast.LENGTH_SHORT);
                }
                else toast = Toast.makeText(this,fail,Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e) {
            Looper.prepare();
            if(sp.getBoolean("logged", false)) {
                Toast.makeText(this, success, Toast.LENGTH_SHORT).show();
            }
            else Toast.makeText(this, fail, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }


}