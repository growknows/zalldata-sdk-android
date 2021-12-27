/*
 * Created by guo on 2016/11/12.
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
package com.zalldata.analytics.android.demo;

import android.app.Application;

import com.zalldata.analytics.android.sdk.ZAConfigOptions;
import com.zalldata.analytics.android.sdk.ZallAnalyticsAutoTrackEventType;
import com.zalldata.analytics.android.sdk.ZallDataAPI;

public class MyApplication extends Application {
    /**
     * Zall Analytics 采集数据的地址
     */
    private final static String ZA_SERVER_URL = "https://sdkdebugtest.datasink.zalldata.cn/sa?project=default&token=cfb8b60e42e0ae9b";

    @Override
    public void onCreate() {
        super.onCreate();
        initZallDataAPI();
    }

    /**
     * 初始化 Zall Analytics SDK
     */
    private void initZallDataAPI() {
        ZAConfigOptions configOptions = new ZAConfigOptions(ZA_SERVER_URL);
        // 打开自动采集, 并指定追踪哪些 AutoTrack 事件
        configOptions.setAutoTrackEventType(ZallAnalyticsAutoTrackEventType.APP_START |
                ZallAnalyticsAutoTrackEventType.APP_END |
                ZallAnalyticsAutoTrackEventType.APP_VIEW_SCREEN |
                ZallAnalyticsAutoTrackEventType.APP_CLICK)
                .enableTrackAppCrash()
                .enableJavaScriptBridge(true)
                .enableVisualizedAutoTrack(true);
        ZallDataAPI.startWithConfigOptions(this, configOptions);
        ZallDataAPI.sharedInstance(this).trackFragmentAppViewScreen();
    }
}
