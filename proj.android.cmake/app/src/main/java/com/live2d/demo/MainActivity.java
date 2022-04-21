

package com.live2d.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;

public class MainActivity extends Activity {
    private GLSurfaceView _glSurfaceView;
    private GLRenderer _glRenderer;
    private DrawerLayout mContainer;
    private ConstraintLayout layout;
    private FrameLayout main;
//    DrawerLayout drawerLayout;
    public SharedPreferences main_sp;

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

    private String speak_result;
    private String last_round;
    private String userName;

    private String return_msg;

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
//        setContentView(R.layout.activity_main);

//        drawerLayout = findViewById(R.id.live2DContainer);

        main = new FrameLayout(this);


        main_sp = getSharedPreferences("login",MODE_PRIVATE);


        //init the live2D
        JniBridgeJava.SetActivityInstance(this);
        JniBridgeJava.SetContext(this);
        _glSurfaceView = new GLSurfaceView(this);
        _glSurfaceView.setEGLContextClientVersion(2);
        //
        _glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        _glSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        _glSurfaceView.setZOrderOnTop(true);
        //
        _glRenderer = new GLRenderer();
        _glSurfaceView.setRenderer(_glRenderer);
        _glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT
                        ? View.SYSTEM_UI_FLAG_LOW_PROFILE
                        : View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        );

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        layout = (ConstraintLayout) inflater.inflate(R.layout.activity_main_view, null);


        ImageButton btn_shift = (ImageButton) layout.findViewById(R.id.btn_shift);
        ImageButton btn_more = (ImageButton) layout.findViewById(R.id.more);
        ImageButton btn_mic = (ImageButton) layout.findViewById(R.id.mic);

        TextView userid = (TextView) layout.findViewById(R.id.userid);
        userName = main_sp.getString("account",null);
        userid.setText(userName);

        main.addView(_glSurfaceView);
        main.addView(layout);

        setContentView(main);


        //Change toolbar title to user's name
//        toolbar_id.setText(main_sp.getString("account", null));
        //init the button


        btn_shift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, ChatActivity1.class));
            }
        });

        //语音输入按钮
        initData();
        myFindViewById();
        mySetOnclickListener();//按钮点击事件处理
    }



    public void ClickZoom(View view) {
        finish();
        startService(new Intent(MainActivity.this, FloatingService.class));
        moveTaskToBack(true);
    }

    public void ClickLogout(View view){
        SharedPreferences.Editor editor = main_sp.edit();
        editor.remove("account").commit();
        editor.putBoolean("logged",false);
        editor.commit();
        boolean flag = main_sp.getBoolean("logged", false);
        Log.d("MUWU", "logged: "+flag);
        String account = main_sp.getString("account", null);
        Log.d("MUWU", "account: "+account);
        Intent i = new Intent(MainActivity.this, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    public static void redirectActivity(Activity activity, Class aClass){
        Intent intent = new Intent(activity, aClass);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        JniBridgeJava.nativeOnStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _glSurfaceView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        _glSurfaceView.onPause();
        JniBridgeJava.nativeOnPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        JniBridgeJava.nativeOnStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        JniBridgeJava.nativeOnDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float pointX = event.getX();
        float pointY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                JniBridgeJava.nativeOnTouchesBegan(pointX, pointY);
                break;
            case MotionEvent.ACTION_UP:
                JniBridgeJava.nativeOnTouchesEnded(pointX, pointY);
                break;
            case MotionEvent.ACTION_MOVE:
                JniBridgeJava.nativeOnTouchesMoved(pointX, pointY);
                break;
        }
        return super.onTouchEvent(event);
    }


    //讯飞
    private void showTip( final String str) {
        mToast.setText(str);
        mToast.show();
    }

    private void initData() {
        context = MainActivity.this;// 初始化识别无UI识别对象/ 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mIat = com.iflytek.cloud.SpeechRecognizer.createRecognizer(context, mInitListener);
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(context, mInitListener);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
        mSharedPreferences = getSharedPreferences(MainActivity.PREFER_NAME,
                Activity.MODE_PRIVATE);
    }

    private void myFindViewById(){
        btStart = (ImageButton) layout.findViewById(R.id.mic);
//        btStop = (Button) findViewById(R.id.btn_stop);
//        btCancel = (Button) findViewById(R.id.btn_cancel);
//        etContent = (EditText) findViewById(R.id.input_text);
    }

    private void mySetOnclickListener() {
        //开始监听按钮事件
        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkSoIsInstallSucceed();
//                etContent.setText(null);// 清空显示内容
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
        speak_result = resultBuffer.toString();
//        etContent.setText(resultBuffer.toString());
//        etContent.setSelection(etContent.length());
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
            new Thread(new Runnable() {
                @Override
                public void run() {
                    if(speak_result!=null)  {
                        return_msg = getHTTPResponse(userName, speak_result, "1");
                        notice(return_msg);
                    }
                }
            }).start();
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


    public String getHTTPResponse(String account, String input_content, String isVoice) {
        String message = "";
        int corpus = 0;
        String scale = "0.4";
        String addr = "http://192.144.215.117:8080/"+account+"/"+last_round+"/"+input_content+"/"+corpus+"/"+isVoice;
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
        showAudioResult(message);
        last_round = message.trim();
        return message;
    }

    private void showAudioResult(String message){
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

    private void notice(String message) {
        Toast toast = null;
        try {
            if(toast!=null){
                toast.setText(message);
            } else {
                toast = Toast.makeText(this,message,Toast.LENGTH_SHORT);
            }
            toast.show();
        } catch (Exception e) {
            Looper.prepare();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

}
