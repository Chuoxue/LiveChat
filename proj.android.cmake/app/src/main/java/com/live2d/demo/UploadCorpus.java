package com.live2d.demo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadCorpus extends AppCompatActivity {

    private static final int READ_REQUEST_CODE = 1337;//自己指定一个intent的请求码，下面用到时解释。
    Uri filePathUri=null; //下面用于存放回传的文件地址。

    private Intent open_intent;

    private String corpus_id="";
    private String userName;

    private ImageButton [] btn_corpus = new ImageButton[4];
    private ImageButton upload;

    private boolean[] isSelected = new boolean[4];

    private EditText[] editTexts;


    private SharedPreferences login_sp;
    private SharedPreferences corpus_sp;

    private int corpus_count;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_corpus);

        login_sp = getSharedPreferences("login",MODE_PRIVATE);
        userName = login_sp.getString("account",null);

        corpus_sp = getSharedPreferences("corpus", MODE_PRIVATE);
        corpus_count = corpus_sp.getInt("count", 1);

        TextView toolbar_id = (TextView) findViewById(R.id.toolbar_id);
        toolbar_id.setText(login_sp.getString("account",null));

        btn_corpus[0] = (ImageButton) findViewById(R.id.btn_corpus1);
        btn_corpus[1] = (ImageButton) findViewById(R.id.btn_corpus2);
        btn_corpus[2] = (ImageButton) findViewById(R.id.btn_corpus3);
        btn_corpus[3] = (ImageButton) findViewById(R.id.btn_corpus4);

        for(int i=0;i<4;i++){
            isSelected[i] = corpus_sp.getBoolean("isSelected"+i, false);
            if(isSelected[i]) {
                btn_corpus[i].setBackgroundResource(R.drawable.rect1);
            } else {
                btn_corpus[i].setBackgroundResource(R.drawable.rect3);
            }
        }

        corpus_sp.edit().putString("0", "0").apply();
        corpus_sp.edit().putString("1", "1").apply();

        upload = (ImageButton) findViewById(R.id.upload);
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                open_intent = new Intent(Intent.ACTION_GET_CONTENT);
//                open_intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/plain"});
//                startActivityForResult(open_intent, 10);

                corpus_count++;
                if(corpus_count<4){
                    corpus_sp.edit().putInt("count",corpus_count).apply();

                    String path = Environment.getExternalStorageDirectory() + "/" + "robot" + "/";
                    Uri uri = Uri.parse(path);
                    Intent intent = new Intent(Intent.ACTION_PICK);
                    intent.setDataAndType(uri, "*/*");
//                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/csv", "text/plain"});
                    startActivityForResult(intent,10);
                } else {
                    Toast.makeText(UploadCorpus.this, "语料库已到达上限", Toast.LENGTH_SHORT).show();
                }


            }
        });

        btn_corpus[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCurrentCorpus(btn_corpus[0], 0);
            }
        });

        btn_corpus[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCurrentCorpus(btn_corpus[1], 1);
            }
        });

        btn_corpus[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCurrentCorpus(btn_corpus[2], 2);
            }
        });

        btn_corpus[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectCurrentCorpus(btn_corpus[3], 3);
            }
        });
    }

    private void selectCurrentCorpus(ImageButton btn_corpus, int btn_id) {
        if(isSelected[btn_id]) {
            btn_corpus.setBackgroundResource(R.drawable.rect3);
            isSelected[btn_id] = false;
            String corpus_id = corpus_sp.getString(btn_id+"","0");
            corpus_sp.edit().putString("corpus_id", corpus_id).apply();
            corpus_sp.edit().putBoolean("isSelected"+btn_id, false).apply();
        } else {
            btn_corpus.setBackgroundResource(R.drawable.rect1);
            String corpus_id = corpus_sp.getString(btn_id+"" , "0");
            corpus_sp.edit().putString("corpus_id", corpus_id).apply();
            isSelected[btn_id] = true;
            corpus_sp.edit().putBoolean("isSelected"+btn_id, true).apply();
            Log.d("muwu", corpus_sp.getString("corpus_id", null));
        }
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 10:
                if (resultCode == RESULT_OK) {
                    String uploadFile = data.getData().getPath();
                    String urlstr = "http://192.144.215.117:8080/uploadCorpus/"+userName;
                    String newName = userName+".csv";
                    new Thread(){
                        @Override
                        public void run() {
                            httpPost(UploadCorpus.this, urlstr, uploadFile, newName);
                            editTexts[corpus_count-2].setText(uploadFile);
                        }
                    }.start();
                }

                break;
        }
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
            FileInputStream fStream = openFileInput(uploadFile);
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
            showDialog(activity,true,uploadFile,"上传成功");
            corpus_id = b.toString().trim();
            corpus_sp.edit().putString(corpus_count+"", corpus_id);

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

    public void ClickBack(View view) { UploadCorpus.this.finish(); }

    public void ClickShift(View view) {startActivity(new Intent(UploadCorpus.this, MainActivity.class));}

}