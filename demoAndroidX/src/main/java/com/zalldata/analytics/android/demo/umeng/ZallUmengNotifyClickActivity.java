package com.zalldata.analytics.android.demo.umeng;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.umeng.message.UmengNotifyClickActivity;

import org.android.agoo.common.AgooConstants;
/**
 * 厂商通道配置托管启动的Activity
 * 如点击小米、华为、OPPO、vivo等系统通道的通知消息，启动的Activity类
 */
public class ZallUmengNotifyClickActivity extends UmengNotifyClickActivity {
    private static final String TAG = "ZallUmengNotifyClickActivity";

    @Override
    public void onMessage(Intent intent) {
        super.onMessage(intent);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            Log.d(TAG, "bundle: " + bundle);
        }
        final String body = intent.getStringExtra(AgooConstants.MESSAGE_BODY);
        Log.d(TAG, "body: " + body);

    }
}
