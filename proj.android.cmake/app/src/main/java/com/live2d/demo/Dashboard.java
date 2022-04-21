package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Dashboard extends AppCompatActivity {

    private String corpus_id = "";
    private SharedPreferences corpus_sp;
    private String userName ="";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        Button btn_upload = (Button)findViewById(R.id.upload);
        Button btn_play = (Button)findViewById(R.id.play);
        Button btn_corpus = (Button)findViewById(R.id.corpus);

        corpus_sp = getSharedPreferences("login",MODE_PRIVATE);
        userName = corpus_sp.getString("account",null);

        btn_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlstr = "http://192.144.215.117:8080/uploadAudio/"+userName;
                String uploadFile = "wxd.wav";
                String newName = "wxd.wav";
                new Thread(){
                    @Override
                    public void run() {
                        httpPost(Dashboard.this, urlstr, uploadFile, newName);
                    }
                }.start();
            }
        });

        btn_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    MediaPlayer player = new MediaPlayer();
                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    player.setDataSource("http://192.144.215.117:8080/audio.wav");
                    player.prepare();
                    player.start();
                } catch (Exception e) {
                    // TODO: handle exception
                }
            }
        });

        //语料
        btn_corpus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String urlstr = "http://192.144.215.117:8080/uploadCorpus/"+userName;
                String uploadFile = "test.csv";
                String newName = "test.csv";
                new Thread(){
                    @Override
                    public void run() {
                        httpPost(Dashboard.this, urlstr, uploadFile, newName);
                    }
                }.start();
            }
        });
    }

    public void httpPost(Activity activity, String urlstr, String uploadFile, String newName) {
//        LogUtil.info("getEhttpPostt", "urlstr="+urlstr+";uploadFile="+uploadFile+";newName="+newName,"i");
        String end = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";//边界标识
        int TIME_OUT = 50*1000;   //超时时间
        HttpURLConnection con = null;
        DataOutputStream ds = null;
        InputStream is = null;
        try {
            URL url = new URL(urlstr);
            con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(TIME_OUT);
            con.setConnectTimeout(TIME_OUT);
            /* 允许Input、Output，不使用Cache */
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            // 设置http连接属性
            con.setRequestMethod("POST");//请求方式
            con.setRequestProperty("Connection", "Keep-Alive");//在一次TCP连接中可以持续发送多份数据而不会断开连接
            con.setRequestProperty("Charset", "GBK");//设置编码
            con.setRequestProperty("Content-Type",//multipart/form-data能上传文件的编码格式
                    "multipart/form-data;boundary=" + boundary);

            ds = new DataOutputStream(con.getOutputStream());
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"stblog\";filename=\"" + newName + "\"" + end);
            ds.writeBytes(end);

            // 取得文件的FileInputStream
//            URL resource = getClassLoader().getResource("assets\\shit.txt");
//            File file = new File(resource.toURI());
//            FileInputStream fStream = new FileInputStream(file);
            File file = new File(uploadFile);
            FileInputStream fStream = new FileInputStream(file);
//            FileInputStream fStream = openFileInput(uploadFile);
            /* 设置每次写入1024bytes */
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            /* 从文件读取数据至缓冲区 */
            while ((length = fStream.read(buffer)) != -1) {
                /* 将资料写入DataOutputStream中 */
                Log.d("muwu",buffer.toString());
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);//结束

            fStream.close();
            ds.flush();
            /* 取得Response内容 */
            is = con.getInputStream();
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            /* 将Response显示于Dialog */
            showDialog(activity,true,uploadFile,"上传成功" + b.toString().trim());
            corpus_id = b.toString().trim();
        } catch (Exception e) {
            showDialog(activity,false,uploadFile,"上传失败" + e);
        }finally {
            /* 关闭DataOutputStream */
            if(ds!=null){
                try {
                    ds.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private static void showDialog(final Activity activity,final Boolean isSuccess,final String uploadFile,final String mess) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(activity).setTitle("Message")
                        .setMessage(mess)
                        .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File file = new File(uploadFile);
                                if(file.exists()&&isSuccess){//日志文件存在且上传日志成功
                                    file.delete();
                                    Toast.makeText(activity, "log日志已删除", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).show();
            }
        });

    }

}