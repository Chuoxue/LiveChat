package com.live2d.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class MsgAdapter extends RecyclerView.Adapter<MsgAdapter.ViewHolder> {
    private List<Msg> list;

    private Context context;
    private SharedPreferences sp;

    public MsgAdapter(List<Msg> list, Context context){
        this.list = list;
        this.context = context;
    }
    static class ViewHolder extends RecyclerView.ViewHolder{
        LinearLayout left;
        LinearLayout leftLayout;
        TextView left_msg;

        LinearLayout right;
        LinearLayout rightLayout;
        TextView right_msg;

        LinearLayout evaluate;
        ImageButton like;
        ImageButton dislike;



        public ViewHolder(View view){
            super(view);
            left = view.findViewById(R.id.left);
            leftLayout = view.findViewById(R.id.left_layout);
            left_msg = view.findViewById(R.id.left_msg);

            evaluate = view.findViewById(R.id.evaluate);
            like = view.findViewById(R.id.like);
            dislike = view.findViewById(R.id.dislike);

            right = view.findViewById(R.id.right);
            rightLayout = view.findViewById(R.id.right_layout);
            right_msg = view.findViewById(R.id.right_msg);

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.msg_item,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Msg msg = list.get(position);
        sp = context.getSharedPreferences("login",MODE_PRIVATE);
        String userName = sp.getString("account", null);
        int like_count;
        int dislike_count;
        if(msg.getType() == Msg.TYPE_RECEIVED){
            //如果是收到的消息，则显示左边的消息布局，将右边的消息布局隐藏
            holder.left_msg.setVisibility(View.VISIBLE);
            holder.left_msg.setText(msg.getContent());
            holder.like.setVisibility(View.VISIBLE);
            holder.dislike.setVisibility(View.VISIBLE);

            //注意此处隐藏右面的消息布局用的是 View.GONE
            holder.rightLayout.setVisibility(View.GONE);

            holder.like.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    holder.like.setBackgroundResource(R.drawable.like);
                    new Thread(){
                        @Override
                        public void run() {
                            updateConfig(userName, "1");
                        }
                    }.start();
                }
            });

            holder.dislike.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.dislike.setBackgroundResource(R.drawable.dislike);
                    new Thread(){
                        @Override
                        public void run() {
                            updateConfig(userName, "0");
                        }
                    }.start();
                }
            });


        }else if(msg.getType() == Msg.TYPE_SEND){
            //如果是发出的消息，则显示右边的消息布局，将左边的消息布局隐藏
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.right_msg.setText(msg.getContent());

            //同样使用View.GONE
            holder.leftLayout.setVisibility(View.GONE);
            holder.like.setVisibility(View.GONE);
            holder.dislike.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


//    public void ClickLike(View view) {
//        like = (ImageButton) findViewById(R.id.like);
//        like.setBackgroundResource(R.drawable.like);
//        new Thread(){
//            @Override
//            public void run() {
//                updateConfig(userName, "1");
//            }
//        }.start();
//    }

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

}


