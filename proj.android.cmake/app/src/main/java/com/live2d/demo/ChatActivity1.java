package com.live2d.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.Image;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ChatActivity1 extends AppCompatActivity {
    private DrawerLayout drawer;
    private static final String TAG = "MainActivity";
    private List<Msg> msgList = new ArrayList<>();
    private RecyclerView msgRecyclerView;
    private EditText inputText;
    private ImageButton send;
    private LinearLayoutManager layoutManager;
    private MsgAdapter adapter;
//    private String input_content;
    private String output_content;
    private String last_round = " ";
    private String isVoice;  //是否语音回复

    private ImageButton like;
    private ImageButton dislike;

    private ImageView gif;

    private TextView user;

    public SharedPreferences chat_sp;
    public SharedPreferences audio_sp;
    private SharedPreferences corpus_sp;
    private String userName = "";
    private String corpus_id;

    private Trie trie;
    private HashMap<String, String> map = new HashMap<>();

    //param for 讯飞SDK
    private Context context;//public static final String PREFER_NAME = "com.voice.recognition";
    public static final String PREFER_NAME = "com.example.speechprojecta";// 语音听写对象
    private SpeechRecognizer mIat;// 语音听写UI
    private RecognizerDialog mIatDialog;// 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private Toast mToast;
//    private Button btStart,btStop,btCancel; Do not have stop and cancel btn now
    private  ImageButton btStart;
    private EditText etContent;
    private SharedPreferences mSharedPreferences;
    private int ret = 0; // 函数调用返回值// 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            LogUtil.v("SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat1);

        trie = new Trie();
        if(read()!=null) {
            String[] content = read().split("@");
            for(int i=0; i<content.length; i+=2) {
                map.put(content[i],content[i+1]);
                trie.addWord(content[i]);
                Log.d("muwu",content[i]);
            }
        }

        gif = (ImageView) findViewById(R.id.gif);
        gif.setVisibility(View.INVISIBLE);
        Glide.with(this).load(R.drawable.loading).into(gif);

        drawer = (DrawerLayout) findViewById(R.id.chatDrawer);
        msgRecyclerView = findViewById(R.id.msg_recycler_view);
        inputText = findViewById(R.id.input_text);
        send = findViewById(R.id.send);
        layoutManager = new LinearLayoutManager(this);
        adapter = new MsgAdapter(msgList = getData(), ChatActivity1.this);

        user = (TextView) findViewById(R.id.user);

        msgRecyclerView.setLayoutManager(layoutManager);
        msgRecyclerView.setAdapter(adapter);

        String input_content = null;

        ImageButton btn_shift = (ImageButton) findViewById(R.id.btn_shift);
        TextView toolbar_id = (TextView) findViewById(R.id.toolbar_id);

        like = (ImageButton) findViewById(R.id.like);
        dislike = (ImageButton) findViewById(R.id.dislike);

        audio_sp = getSharedPreferences("audio", MODE_PRIVATE);
        corpus_sp =getSharedPreferences("corpus", MODE_PRIVATE);

        corpus_id = corpus_sp.getString("corpus_id", "0");

        chat_sp = getSharedPreferences("login",MODE_PRIVATE);
        userName = chat_sp.getString("account", null);

        user.setText(userName);
        toolbar_id.setText(userName);
        Log.d("muwu","user"+userName);

        btn_shift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(ChatActivity1.this, MainActivity.class));
            }
        });

/*       为button建立一个监听器，将编辑框的内容发送到 RecyclerView 上：
            ①获取内容，将需要发送的消息添加到 List 当中去。
            ②调用适配器的notifyItemInserted方法，通知有新的数据加入了，赶紧将这个数据加到 RecyclerView 上面去。
            ③调用RecyclerView的scrollToPosition方法，以保证一定可以看的到最后发出的一条消息。*/
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gif.setVisibility(View.VISIBLE);
                final String input_content = inputText.getText().toString();
                isVoice = audio_sp.getString("isVoice", "0");
                Log.d("muwu", "isVoice: "+isVoice);
                KeyListener keyListener = inputText.getKeyListener();
                if(!input_content.equals("")) {
                    msgList.add(new Msg(input_content,Msg.TYPE_SEND));
                    adapter.notifyItemInserted(msgList.size()-1);
                    msgRecyclerView.scrollToPosition(msgList.size()-1);
                    inputText.setText("");//清空输入框中的内容

                    inputText.setKeyListener(null);
//                    inputText.setFocusable(false);
//                    inputText.setFocusableInTouchMode(false);
                }
                Log.d("muwu",trie.filter(input_content));
                if(map.get(trie.filter(input_content))!=null) {
                    Log.d("muwu","keyword: "+trie.filter(input_content));
                    showResult(map.get(trie.filter(input_content)));
                    inputText.setKeyListener(keyListener);
                    gif.setVisibility(View.INVISIBLE);
                }
                else {
                    new Thread(){
                        @Override
                        public void run() {
                            getHTTPResponse(userName, input_content, corpus_id, isVoice);
                            if(isVoice.equals("1")) {
                                try {
                                    MediaPlayer player = new MediaPlayer();
                                    player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                                    player.setDataSource("http://192.144.215.117:8080/download/"+userName+"/audio.wav");
                                    player.prepare();
                                    player.start();
                                    Log.d("muwu","play audio");
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            inputText.setKeyListener(keyListener);
                            gif.setVisibility(View.INVISIBLE);
                        }
                    }.start();
                }

//                inputText.setFocusableInTouchMode(true);
//                inputText.setFocusable(true);
//                inputText.requestFocus();

//                if(msgList.size()%2==0){
//                    msgList.add(new Msg(output_content,Msg.TYPE_RECEIVED));
//                    adapter.notifyItemInserted(msgList.size()-1);
//                    msgRecyclerView.scrollToPosition(msgList.size()-1);
//                }
            }
        });




        initData();
        myFindViewById();
        mySetOnclickListener();//按钮点击事件处理
    }

    //侧拉栏
    public void ClickMenu(View view){
        openDrawer(drawer);
    }

    public static void openDrawer(DrawerLayout drawerLayout){
        //Open drawer layout
        drawerLayout.openDrawer(GravityCompat.START);
    }

    public void ClickLogo(View view){
        //Close drawer
        closeDrawer(drawer);
    }

    public static void closeDrawer(DrawerLayout drawerLayout){
        //Close drawer layout
        //Check condition
        if(drawerLayout.isDrawerOpen(GravityCompat.START)){
            //When it's open, close
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void ClickHome(View view){
        recreate();
    }

    public void ClickAudio(View view){redirectActivity(this, UploadAudio.class);}

    public void ClickCorpus(View view) { redirectActivity(this, UploadCorpus.class);}

    public void ClickBase(View view) { redirectActivity(this, BaseConfig.class);}

    public void ClickConfig(View view) { redirectActivity(this, ConfigureActivity.class);}

    public void ClickPrivate(View view) { redirectActivity(this, Privacy.class);}

    public void ClickHelp(View view) { redirectActivity(this, Help.class);}

    public void ClickLogout(View view){
        SharedPreferences.Editor editor = chat_sp.edit();
        editor.remove("account").commit();
        editor.putBoolean("logged",false);
        editor.commit();

        SharedPreferences base_sp = getSharedPreferences("base", MODE_PRIVATE);
        base_sp.edit().putBoolean("first_time", true).commit();

        SharedPreferences config_sp = getSharedPreferences("config", MODE_PRIVATE);
        config_sp.edit().remove("progress1").commit();
        config_sp.edit().remove("progress2").commit();
        config_sp.edit().remove("progress3").commit();

        Intent i = new Intent(ChatActivity1.this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public static void redirectActivity(Activity activity, Class aClass){
        Intent intent = new Intent(activity, aClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    //聊天
    private List<Msg> getData(){
        List<Msg> list = new ArrayList<>();
        list.add(new Msg("Hello",Msg.TYPE_RECEIVED));
        return list;
    }

    public String getHTTPResponse(String account, String input_content, String corpus_id, String isVoice) {
        String message = "";
        String scale = "0.4";
        String addr = "http://192.144.215.117:8080/"+account+"/"+last_round+"/"+input_content+"/"+corpus_id+"/"+isVoice;
        Log.d("MUWU", "addr: "+addr);
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
            message = sb.toString().trim();
            inputStream.close();
            connection.disconnect();
            Log.d("MUWU", "message: " + message);
        } catch (Exception e) {
            Log.d("MUWU", e.getMessage());
            e.printStackTrace();
        }
        showResult(message);
        last_round = message.trim();
        return message;
    }

    public void updateConfig(String account, String thumbs) {
        String init = "0";
        String threshold_c = "0";
        String threshold_e = "0";
        String emo_pref = "0";
        String addr = "http://192.144.215.117:8080/"+account+"/"+init+"/"+threshold_c+"/"
                +threshold_e+"/"+emo_pref+"/"+thumbs;
        Log.d("MUWU", "addr: "+addr);
        try {
            URL url = new URL(addr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5 * 1000);
            connection.connect();
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showResult(final String result){
        output_content = result;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(msgList.size()%2==0){
                    msgList.add(new Msg(output_content,Msg.TYPE_RECEIVED));
                    adapter.notifyItemInserted(msgList.size()-1);
                    msgRecyclerView.scrollToPosition(msgList.size()-1);
                }
            }
        });
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




    //讯飞
    private void showTip( final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void initData() {
        context = ChatActivity1.this;// 初始化识别无UI识别对象/ 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = com.iflytek.cloud.SpeechRecognizer.createRecognizer(context, mInitListener);
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(context, mInitListener);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mSharedPreferences = getSharedPreferences(ChatActivity1.PREFER_NAME,
                Activity.MODE_PRIVATE);
    }

    private void myFindViewById(){
        btStart = (ImageButton) findViewById(R.id.speaker);
//        btStop = (Button) findViewById(R.id.btn_stop);
//        btCancel = (Button) findViewById(R.id.btn_cancel);
        etContent = (EditText) findViewById(R.id.input_text);
    }

    private void mySetOnclickListener() {
        //开始监听按钮事件
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSoIsInstallSucceed();
                etContent.setText(null);// 清空显示内容
                mIatResults.clear();
                // 设置参数
                setParam();
                // 默认不显示出讯飞的图像
                boolean isShowDialog = mSharedPreferences.getBoolean(
                        getString(R.string.pref_key_iat_show), false);
                if (isShowDialog) {
                    // 显示听写对话框
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();
                    showTip(getString(R.string.text_begin));
                } else {
                    // 不显示听写对话框
                    ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        showTip("听写失败,错误码：" + ret);
                    } else {
                        showTip(getString(R.string.text_begin));
                    }
                }
            }
        });

//        btStop.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                checkSoIsInstallSucceed();
//                mIat.stopListening();
//                showTip("停止听写");
//            }
//        });
//
//        btCancel.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                checkSoIsInstallSucceed();
//                mIat.cancel();
//                showTip("取消听写");
//            }
//        });
    }

    //检查是否成功创建对象
    private void checkSoIsInstallSucceed(){
        if( null == mIat ){
            // 创建单例失败，与 21001 错误为同样原因，参考 http://bbs.xfyun.cn/forum.php?mod=viewthread&tid=9688
            this.showTip( "创建对象失败，请确认 libmsc.so 放置正确，且有调用 createUtility 进行初始化" );
            return;
        }
    }

    //设置参数
    public void setParam() {
        mIat.setParameter(SpeechConstant.PARAMS, null); // 清空参数
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType); // 设置听写引擎
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");  // 设置返回结果格式

        // 设定为只理解中文
        // 设置语言
        mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        // 设置语言区域
        mIat.setParameter(SpeechConstant.ACCENT, "mandarin");

        // 下方的SharedPreferences都是可以删除的，考虑可以在时候的设置中进行手动更改。
        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));
        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));
        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "0"));
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    //识别回调函数
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            printResult(results);
            String text1 = JsonParser.parseIatResult(results.getResultString());
            System.out.println(text1);
            String tt="开灯";
            // Toast.makeText(context, text1, Toast.LENGTH_LONG).show();
            if (text1.equals(tt)){
                Toast.makeText(context,"ninhao",Toast.LENGTH_SHORT).show();
            }
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
        }

    };

    //打印听写识别结果
    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());
        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mIatResults.put(sn, text);
        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }
        Log.d("MUWU", resultBuffer.toString());
        etContent.setText(resultBuffer.toString());
        etContent.setSelection(etContent.length());
    }

    /**
     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }
        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            // 如果使用本地功能（语记）需要提示用户开启语记的录音权限。
            showTip(error.getPlainDescription(true));
        }
        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }
        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            LogUtil.v(results.getResultString());
            printResult(results);
        }
        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            LogUtil.v("返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };
}