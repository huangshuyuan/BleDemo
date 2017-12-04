package com.skypine.ble.client.utils;

import android.content.Context;

import java.util.ArrayList;

/**
 * Author: syhuang
 * Date:  2017/11/21
 */

public class ReceiverUtil {

    private static ReceiverUtil instance;
    private        Context      mContext;

    private final static ArrayList<ReceiveListener> mListenerArrayList = new ArrayList<>();

    public static ArrayList<ReceiveListener> getListenerArrayList() {
        return mListenerArrayList;
    }

    private ReceiverUtil(Context context) {
        this.mContext = context;
        //TODO

    }

    public synchronized static ReceiverUtil getInstance(Context context) {
        if (instance == null) {
            instance = new ReceiverUtil(context);
        }
        return instance;
    }

    public void addListener(ReceiveListener listener) {
        if (!mListenerArrayList.contains(listener)) {
            mListenerArrayList.add(listener);
        }
    }

    public void removeListener(ReceiveListener listener) {
        if (mListenerArrayList.contains(listener)) {
            mListenerArrayList.remove(listener);
        }
    }

}
