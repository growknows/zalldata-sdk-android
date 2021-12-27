/*
 * Created by guo on 2020/07/06.
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

package com.zalldata.analytics.android.sdk.deeplink;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.ServerUrl;
import com.zalldata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.zalldata.analytics.android.sdk.advert.utils.OaidHelper;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class DeepLinkManager {
    public static final String IS_ANALYTICS_DEEPLINK = "is_analytics_deeplink";
    private static DeepLinkProcessor mDeepLinkProcessor;

    public enum DeepLinkType {
        CHANNEL,
        ZALLDATA
    }

    /**
     * 是否是 DeepLink 唤起
     *
     * @param intent Intent
     * @return 是否是 DeepLink 唤起
     */
    private static boolean isDeepLink(Intent intent) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && intent != null && Intent.ACTION_VIEW.equals(intent.getAction());
    }

    /**
     * 是否是是 UtmDeepLink
     *
     * @param intent Intent
     * @return 是否是 UtmDeepLink
     */
    private static boolean isUtmDeepLink(Intent intent) {
        if (!isDeepLink(intent) || intent.getData() == null) {
            return false;
        }
        Uri uri = intent.getData();
        if (uri.isOpaque()) {
            ZALog.d("ChannelDeepLink", uri.toString() + " isOpaque");
            return false;
        }
        Set<String> parameterNames = uri.getQueryParameterNames();
        if (parameterNames != null && parameterNames.size() > 0) {
            return ChannelUtils.hasLinkUtmProperties(parameterNames);
        }
        return false;
    }

    /**
     * 是否是卓尔 DeepLink
     *
     * @param serverHost 数据接收地址 host
     * @param intent DeepLink 唤起的 Intent
     * @return 是否是卓尔 DeepLink
     */
    private static boolean isZallDataDeepLink(Intent intent, String serverHost) {
        if (!isDeepLink(intent) || TextUtils.isEmpty(serverHost) || intent.getData() == null) {
            return false;
        }
        Uri uri = intent.getData();
        List<String> paths = uri.getPathSegments();
        if (paths != null && !paths.isEmpty()) {
            if (paths.get(0).equals("sd")) {
                String host = uri.getHost();
                return !TextUtils.isEmpty(host) && (host.equals(serverHost) || host.equals("zalldata"));
            }
        }
        return false;
    }

    private static DeepLinkProcessor createDeepLink(Intent intent, String serverUrl) {
        if (intent == null) {
            return null;
        }
        //优先判断是否是卓尔 DeepLink 短链
        if (isZallDataDeepLink(intent, new ServerUrl(serverUrl).getHost())) {
            return new ZallDataDeepLink(intent, serverUrl);
        }
        if (isUtmDeepLink(intent)) {
            return new ChannelDeepLink(intent);
        }
        return null;
    }

    private static void trackDeepLinkLaunchEvent(final Context context, DeepLinkProcessor deepLink) {
        final JSONObject properties = new JSONObject();
        final ZallDataAPI zallDataAPI = ((ZallDataAPI) ZallDataAPI.sharedInstance());
        final boolean isDeepLinkInstallSource = deepLink instanceof ZallDataDeepLink && zallDataAPI.isDeepLinkInstallSource();
        try {
            properties.put("$deeplink_url", deepLink.getDeepLinkUrl());
            properties.put("$time", new Date(System.currentTimeMillis()));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
        ZallDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), properties);
        ZallDataUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
        zallDataAPI.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                if (isDeepLinkInstallSource) {
                    try {
                        properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(context,
                                ZallDataUtils.getAndroidID(context), OaidHelper.getOAID(context)));
                    } catch (JSONException e) {
                        ZALog.printStackTrace(e);
                    }
                }
                zallDataAPI.trackInternal("$AppDeeplinkLaunch", properties);
            }
        });
    }

    public interface OnDeepLinkParseFinishCallback {
        void onFinish(DeepLinkType deepLinkStatus, String pageParams, boolean success, long duration);
    }

    public static boolean parseDeepLink(final Activity activity, final boolean isSaveDeepLinkInfo, final ZallDataDeepLinkCallback callback) {
        try {
            Intent intent = activity.getIntent();
            mDeepLinkProcessor = createDeepLink(intent, ZallDataAPI.sharedInstance().getServerUrl());
            if (mDeepLinkProcessor == null) {
                return false;
            }
            //清除本地 utm 属性
            ChannelUtils.clearUtm(activity.getApplicationContext());
            // 注册 DeepLink 解析完成 callback.
            mDeepLinkProcessor.setDeepLinkParseFinishCallback(new OnDeepLinkParseFinishCallback() {
                @Override
                public void onFinish(DeepLinkType deepLinkStatus, String params, boolean success, long duration) {
                    if (isSaveDeepLinkInfo) {
                        ChannelUtils.saveDeepLinkInfo(activity.getApplicationContext());
                    }
                    if (callback != null && deepLinkStatus == DeepLinkType.ZALLDATA) {
                        callback.onReceive(params, success, duration);
                    }
                }
            });
            mDeepLinkProcessor.parseDeepLink(intent);
            //触发 $AppDeeplinkLaunch 事件
            DeepLinkManager.trackDeepLinkLaunchEvent(activity.getApplicationContext(), mDeepLinkProcessor);
            return true;
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return false;
    }

    /**
     * 合并渠道信息到 properties 中
     *
     * @param properties 属性
     */
    public static void mergeDeepLinkProperty(JSONObject properties) {
        try {
            if (mDeepLinkProcessor != null) {
                mDeepLinkProcessor.mergeDeepLinkProperty(properties);
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 重置 DeepLink 解析器
     */
    public static void resetDeepLinkProcessor() {
        mDeepLinkProcessor = null;
    }
}
