/*
 * Created by guo on 2015/08/01.
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

package com.zalldata.analytics.android.sdk;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.zalldata.analytics.android.sdk.util.ReflectUtil;
import com.zalldata.analytics.android.sdk.visual.property.VisualPropertiesManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/* package */ class AppWebViewInterface {
    private static final String TAG = "ZA.AppWebViewInterface";
    private Context mContext;
    private JSONObject properties;
    private boolean enableVerify;
    private WeakReference<View> mWebView;

    AppWebViewInterface(Context c, JSONObject p, boolean b) {
        this(c, p, b, null);
    }

    AppWebViewInterface(Context c, JSONObject p, boolean b, View view) {
        this.mContext = c;
        this.properties = p;
        this.enableVerify = b;
        if (view != null) {
            this.mWebView = new WeakReference<>(view);
        }
    }

    @JavascriptInterface
    public String zalldata_call_app() {
        try {
            if (properties == null) {
                properties = new JSONObject();
            }
            properties.put("type", "Android");
            String loginId = ZallDataAPI.sharedInstance(mContext).getLoginId();
            if (!TextUtils.isEmpty(loginId)) {
                properties.put("distinct_id", loginId);
                properties.put("is_login", true);
            } else {
                properties.put("distinct_id", ZallDataAPI.sharedInstance(mContext).getAnonymousId());
                properties.put("is_login", false);
            }
            return properties.toString();
        } catch (JSONException e) {
            ZALog.i(TAG, e.getMessage());
        }
        return null;
    }

    @JavascriptInterface
    public void zalldata_track(String event) {
        try {
            ZallDataAPI.sharedInstance(mContext).trackEventFromH5(event, enableVerify);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @JavascriptInterface
    public boolean zalldata_verify(String event) {
        try {
            if (!enableVerify) {
                zalldata_track(event);
                return true;
            }
            return ZallDataAPI.sharedInstance(mContext)._trackEventFromH5(event);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
            return false;
        }
    }

    @JavascriptInterface
    public String zalldata_get_server_url() {
        return ZallDataAPI.sharedInstance().getConfigOptions().isAutoTrackWebView ? ZallDataAPI.sharedInstance().getServerUrl() : "";
    }

    /**
     * 解决用户只调用了 showUpWebView 方法时，此时 App 校验 url。JS 需要拿到 App 校验结果。
     *
     * @param event
     * @return
     */
    @JavascriptInterface
    public boolean zalldata_visual_verify(String event) {
        try {
            if (!enableVerify) {
                return true;
            }
            if (TextUtils.isEmpty(event)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(event);
            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(ZallDataAPI.sharedInstance().getServerUrl())))) {
                    return false;
                }
                return true;
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 新打通方案下 js 调用 app 的通道,该接口可复用
     *
     * @param content JS 发送的消息
     */
    @JavascriptInterface
    public void zalldata_js_call_app(final String content) {
        try {
            if (mWebView != null) {
                ZallDataAPI.sharedInstance().handleJsMessage(mWebView, content);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 判断 A/B Testing 是否初始化
     *
     * @return A/B Testing SDK 是否初始化
     */
    @JavascriptInterface
    public boolean zalldata_abtest_module() {
        try {
            Class<?> zallABTestClass = ReflectUtil.getCurrentClass(new String[]{"com.zalldata.abtest.ZallABTest"});
            Object object = ReflectUtil.callStaticMethod(zallABTestClass, "shareInstance");
            return object != null;
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * JS 从 App 侧获取结果的统一入口
     * 该接口的设计上和 'zalldata_js_call_app' 接口有所重复，'zalldata_js_call_app' 接口设计存在缺陷，没法有返回值。
     * 本次新增该接口适用于有返回值场景
     *
     * @return 同步返回给 JS 侧结果
     */
    @JavascriptInterface
    public String zalldata_get_app_visual_config() {
        try {
            // 可视化的开关未打开，此时 App 内嵌 H5 场景无需支持采集自定义属性；
            if (!ZallDataAPI.getConfigOptions().isVisualizedPropertiesEnabled()) {
                return null;
            }
            VisualPropertiesManager.getInstance().getVisualPropertiesH5Helper().registerListeners();
            String visualCache = VisualPropertiesManager.getInstance().getVisualPropertiesCache().getVisualCache();
            if (!TextUtils.isEmpty(visualCache)) {
                return Base64.encodeToString(visualCache.getBytes(), Base64.DEFAULT);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return null;
    }
}
