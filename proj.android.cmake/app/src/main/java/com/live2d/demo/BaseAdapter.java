package com.live2d.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class BaseAdapter extends RecyclerView.Adapter<BaseAdapter.BaseViewHolder>{

    private String[] str;

    public BaseAdapter(String [] str) {
        this.str = str;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.table_item, parent, false);
        return new BaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseAdapter.BaseViewHolder holder, int position) {
        holder.key.setText(str[position*2]);
        holder.value.setText(str[position*2+1]);
        holder.line.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() { return str.length/2;}

    public class BaseViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout line;
        private TextView key;
        private TextView value;

        public BaseViewHolder(@NonNull View itemView) {
            super(itemView);

            line = itemView.findViewById(R.id.line);
            key = itemView.findViewById(R.id.key);
            value = itemView.findViewById(R.id.value);
        }
    }
}

