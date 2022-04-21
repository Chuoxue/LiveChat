package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class BaseConfig extends AppCompatActivity {

    private HashMap<String, String> hashMap;
    private String full_content;

    private EditText key_text;
    private EditText value_text;

    private String cur_key;
    private String cur_value;

    private BaseAdapter baseAdapter;

    private RecyclerView table_rv;

    private SharedPreferences base_sp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_config);

        base_sp = getSharedPreferences("base", MODE_PRIVATE);

        if(base_sp.getBoolean("first_time", true)) save("关键词@对应回复");
        base_sp.edit().putBoolean("first_time", false).apply();

        full_content = read();
        if(full_content!=null) uploadTable();

        ImageButton confirm_btn = (ImageButton) findViewById(R.id.confirm);

        TextView toolbar_id = (TextView) findViewById(R.id.toolbar_id);
        SharedPreferences login_sp = getSharedPreferences("login", MODE_PRIVATE);
        toolbar_id.setText(login_sp.getString("account",null));

        key_text = (EditText) findViewById(R.id.key_text);
        value_text = (EditText) findViewById(R.id.value_text);

        confirm_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cur_key = key_text.getText().toString();
                cur_value = value_text.getText().toString();
                full_content = read();
                full_content = full_content+"@"+cur_key+"@"+cur_value;
                save(full_content);
                uploadTable();
                Log.d("muwu","click");
            }
        });

    }


    private void save(String content){
        FileOutputStream fileOutputStream=null;
        try {
            fileOutputStream=openFileOutput("text.txt",MODE_PRIVATE);
            fileOutputStream.write(content.getBytes());
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fileOutputStream!=null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String read(){
        FileInputStream fileInputStream=null;
        try {
            fileInputStream=openFileInput("text.txt");
            byte[] buff=new byte[1024];//计算机处理数据的单位就是字节，读取文件的1024字节长度的信息，byte数组相当于缓存，要循环去进行读写的
            StringBuilder stringBuilder=new StringBuilder("");//用来拼接，StringBuffer是线程安全的，而StringBuilder则没有实现线程安全功能，所以性能略高
            int len=0;
            while((len=fileInputStream.read(buff))>0){
                stringBuilder.append(new String(buff,0,len));//从0到len读取的1024个字节，直到text.txt文件中的数据读完
            }
            return stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fileInputStream!=null){
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    private void uploadTable(){
        String content [] = read().split("@");
        Log.d("muwu",read());
        Log.d("muwu",content.length+"");
        baseAdapter = new BaseAdapter(content);
        table_rv = (RecyclerView) findViewById(R.id.table_recycler_view);
        table_rv.setLayoutManager(new LinearLayoutManager(this));
        table_rv.setAdapter(baseAdapter);
        Log.d("muwu",baseAdapter.getItemCount()+"");
    }


    public void ClickBack(View view) { BaseConfig.this.finish(); }

    public void ClickShift(View view) {startActivity(new Intent(BaseConfig.this, MainActivity.class));}
}