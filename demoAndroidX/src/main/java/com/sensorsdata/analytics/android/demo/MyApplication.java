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
package com.sensorsdata.analytics.android.demo;

import android.app.Application;

import com.sensorsdata.analytics.android.sdk.SAConfigOptions;
import com.sensorsdata.analytics.android.sdk.SensorsAnalyticsAutoTrackEventType;
import com.sensorsdata.analytics.android.sdk.SensorsDataAPI;

public class MyApplication extends Application {
    /**
     * Zall Data 采集数据的地址
     */
    private final static String SA_SERVER_URL = "http://172.16.90.61:58080/a?service=zall&project=dddssss";

    @Override
    public void onCreate() {
        super.onCreate();
        initSensorsDataAPI();
    }

    /**
     * 初始化 Zall Data SDK
     */
    private void initSensorsDataAPI() {
        SAConfigOptions configOptions = new SAConfigOptions(SA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(SensorsAnalyticsAutoTrackEventType.APP_START |
                SensorsAnalyticsAutoTrackEventType.APP_END |
                SensorsAnalyticsAutoTrackEventType.APP_VIEW_SCREEN |
                SensorsAnalyticsAutoTrackEventType.APP_CLICK)
                .enableTrackAppCrash()
                .enableJavaScriptBridge(true)
                .enableVisualizedAutoTrack(true)
                .enableLog(true)
                .enableTrackPageLeave(true)
                .setFlushBulkSize(1)
                .enableTrackScreenOrientation(true);

        SensorsDataAPI.startWithConfigOptions(this, configOptions);
        SensorsDataAPI.sharedInstance(this).trackFragmentAppViewScreen();
    }
}
