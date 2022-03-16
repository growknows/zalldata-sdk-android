package com.zalldata.analytics.android.demo.jpush;

import android.content.Context;
import android.util.Log;

import cn.jpush.android.api.CustomMessage;
import cn.jpush.android.api.NotificationMessage;
import cn.jpush.android.service.JPushMessageReceiver;

import static android.content.ContentValues.TAG;

public class XReceiver extends JPushMessageReceiver {

    @Override
    public void onNotifyMessageOpened(Context context, NotificationMessage notificationMessage) {
//        super.onNotifyMessageOpened(context, notificationMessage);
        Log.d(TAG, "onNotifyMessageOpened: ");
    }

    @Override
    public void onMessage(Context context, CustomMessage customMessage) {
        super.onMessage(context, customMessage);
        Log.d(TAG, "onMessage: ");
    }

    @Override
    public void onRegister(Context context, String s) {
        super.onRegister(context, s);
        Log.d(TAG, "onRegister: " + s);

    }
}
