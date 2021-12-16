/*
 * Created by guo on 2020/4/27.
 * Copyright 2015－2021 Zall Data Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zalldata.analytics.android.sdk.internal.rpc;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.data.adapter.DbAdapter;
import com.zalldata.analytics.android.sdk.data.adapter.DbParams;

/**
 * 用于跨进程业务的数据通信
 */
public class ZallDataContentObserver extends ContentObserver {
    public static boolean isEnableFromObserver = false;
    public static boolean isDisableFromObserver = false;
    public static boolean isLoginFromObserver = false;

    public ZallDataContentObserver() {
        super(new Handler(Looper.getMainLooper()));
    }

    @Override
    public void onChange(boolean selfChange, Uri uri) {
        try {
            if (DbParams.getInstance().getDataCollectUri().equals(uri)) {
                ZallDataAPI.sharedInstance().enableDataCollect();
            } else if (DbParams.getInstance().getSessionTimeUri().equals(uri)) {
                ZallDataAPI.sharedInstance().setSessionIntervalTime(DbAdapter.getInstance().getSessionIntervalTime());
            } else if (DbParams.getInstance().getLoginIdUri().equals(uri)) {
                String loginId = DbAdapter.getInstance().getLoginId();
                if (TextUtils.isEmpty(loginId)) {
                    ZallDataAPI.sharedInstance().logout();
                } else {
                    isLoginFromObserver = true;
                    ZallDataAPI.sharedInstance().login(loginId);
                }
            } else if (DbParams.getInstance().getDisableSDKUri().equals(uri)) {
                if (!ZallDataAPI.getConfigOptions().isDisableSDK()) {
                    isDisableFromObserver = true;
                    ZallDataAPI.disableSDK();
                }
            } else if (DbParams.getInstance().getEnableSDKUri().equals(uri)) {
                if (ZallDataAPI.getConfigOptions().isDisableSDK()) {
                    isEnableFromObserver = true;
                    ZallDataAPI.enableSDK();
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }
}