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

        //??????
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
        String boundary = "*****";//????????????
        int TIME_OUT = 50*1000;   //????????????
        HttpURLConnection con = null;
        DataOutputStream ds = null;
        InputStream is = null;
        try {
            URL url = new URL(urlstr);
            con = (HttpURLConnection) url.openConnection();
            con.setReadTimeout(TIME_OUT);
            con.setConnectTimeout(TIME_OUT);
            /* ??????Input???Output????????????Cache */
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);

            // ??????http????????????
            con.setRequestMethod("POST");//????????????
            con.setRequestProperty("Connection", "Keep-Alive");//?????????TCP????????????????????????????????????????????????????????????
            con.setRequestProperty("Charset", "GBK");//????????????
            con.setRequestProperty("Content-Type",//multipart/form-data??????????????????????????????
                    "multipart/form-data;boundary=" + boundary);

            ds = new DataOutputStream(con.getOutputStream());
            ds.writeBytes(twoHyphens + boundary + end);
            ds.writeBytes("Content-Disposition: form-data; "
                    + "name=\"stblog\";filename=\"" + newName + "\"" + end);
            ds.writeBytes(end);

            // ???????????????FileInputStream
//            URL resource = getClassLoader().getResource("assets\\shit.txt");
//            File file = new File(resource.toURI());
//            FileInputStream fStream = new FileInputStream(file);
            File file = new File(uploadFile);
            FileInputStream fStream = new FileInputStream(file);
//            FileInputStream fStream = openFileInput(uploadFile);
            /* ??????????????????1024bytes */
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int length = -1;
            /* ????????????????????????????????? */
            while ((length = fStream.read(buffer)) != -1) {
                /* ???????????????DataOutputStream??? */
                Log.d("muwu",buffer.toString());
                ds.write(buffer, 0, length);
            }
            ds.writeBytes(end);
            ds.writeBytes(twoHyphens + boundary + twoHyphens + end);//??????

            fStream.close();
            ds.flush();
            /* ??????Response?????? */
            is = con.getInputStream();
            int ch;
            StringBuffer b = new StringBuffer();
            while ((ch = is.read()) != -1) {
                b.append((char) ch);
            }
            /* ???Response?????????Dialog */
            showDialog(activity,true,uploadFile,"????????????" + b.toString().trim());
            corpus_id = b.toString().trim();
        } catch (Exception e) {
            showDialog(activity,false,uploadFile,"????????????" + e);
        }finally {
            /* ??????DataOutputStream */
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
                        .setNegativeButton("??????", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                File file = new File(uploadFile);
                                if(file.exists()&&isSuccess){//???????????????????????????????????????
                                    file.delete();
                                    Toast.makeText(activity, "log???????????????", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }).show();
            }
        });

    }

}