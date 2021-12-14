/*
 * Created by guo on 2020/4/20.
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
package com.zalldata.zall.android.demo;

import android.app.Application;

import com.zalldata.zall.android.sdk.ZAConfigOptions;
import com.zalldata.zall.android.sdk.ZallDataAutoTrackEventType;
import com.zalldata.zall.android.sdk.ZallDataAPI;

public class MyApplication extends Application {
    /**
     * Zall Data 采集数据的地址
     */
    private final static String ZA_SERVER_URL = "http://172.16.90.61:58080/a?service=zall&project=dddssss";

    @Override
    public void onCreate() {
        super.onCreate();
        initZallDataAPI();
    }

    /**
     * 初始化 Zall Data SDK
     */
    private void initZallDataAPI() {
        ZAConfigOptions configOptions = new ZAConfigOptions(ZA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(ZallDataAutoTrackEventType.APP_START |
                ZallDataAutoTrackEventType.APP_END |
                ZallDataAutoTrackEventType.APP_VIEW_SCREEN |
                ZallDataAutoTrackEventType.APP_CLICK)
                .enableTrackAppCrash()
                .enableJavaScriptBridge(true)
                .enableVisualizedAutoTrack(true)
                .enableLog(true)
                .enableTrackPageLeave(true)
                .setFlushBulkSize(1)
                .enableTrackScreenOrientation(true);

        ZallDataAPI.startWithConfigOptions(this, configOptions);
        ZallDataAPI.sharedInstance(this).trackFragmentAppViewScreen();
    }
}
