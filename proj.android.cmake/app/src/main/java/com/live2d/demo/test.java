package com.live2d.demo;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

public class test implements Parcelable {


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public void readFromParcel(Parcel source) {
    }

    public test() {
    }

    protected test(Parcel in) {
    }

    public static final Parcelable.Creator<test> CREATOR = new Parcelable.Creator<test>() {
        @Override
        public test createFromParcel(Parcel source) {
            return new test(source);
        }

        @Override
        public test[] newArray(int size) {
            return new test[size];
        }
    };
}
