/*
 * Created by guo on 2021/1/12.
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

package com.zalldata.analytics.android.sdk.remote;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

import com.zalldata.analytics.android.sdk.ZAConfigOptions;
import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.encrypt.ZAEncryptListener;
import com.zalldata.analytics.android.sdk.encrypt.SecreteKey;
import com.zalldata.analytics.android.sdk.encrypt.ZallDataEncrypt;
import com.zalldata.analytics.android.sdk.network.HttpCallback;
import com.zalldata.analytics.android.sdk.network.HttpMethod;
import com.zalldata.analytics.android.sdk.network.RequestHelper;
import com.zalldata.analytics.android.sdk.util.AppInfoUtils;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;

public abstract class BaseZallDataSDKRemoteManager {

    protected static final String TAG = "ZA.ZallDataSDKRemoteConfigBase";
    protected Context mContext;
    protected ZAConfigOptions mZAConfigOptions;
    protected ZallDataEncrypt mZallDataEncrypt;
    protected boolean mDisableDefaultRemoteConfig;

    protected static ZallDataSDKRemoteConfig mSDKRemoteConfig;
    protected ZallDataAPI mZallDataAPI;

    protected BaseZallDataSDKRemoteManager(ZallDataAPI zallDataAPI) {
        this.mZallDataAPI = zallDataAPI;
        this.mContext = zallDataAPI.getContext();
        this.mZAConfigOptions = zallDataAPI.getConfigOptions();
        this.mZallDataEncrypt = zallDataAPI.getZallDataEncrypt();
        this.mDisableDefaultRemoteConfig = zallDataAPI.isDisableDefaultRemoteConfig();
    }


    public abstract void pullSDKConfigFromServer();

    public abstract void requestRemoteConfig(RandomTimeType randomTimeType, final boolean enableConfigV);

    public abstract void resetPullSDKConfigTimer();

    /**
     * 从本地缓存中读取最新的 SDK 配置信息
     */
    public abstract void applySDKConfigFromCache();

    protected abstract void setSDKRemoteConfig(ZallDataSDKRemoteConfig sdkRemoteConfig);

    public boolean ignoreEvent(String eventName) {
        if (mSDKRemoteConfig != null && mSDKRemoteConfig.getEventBlacklist() != null) {
            try {
                int size = mSDKRemoteConfig.getEventBlacklist().length();
                for (int i = 0; i < size; i++) {
                    if (eventName.equals(mSDKRemoteConfig.getEventBlacklist().get(i))) {
                        ZALog.i(TAG, "remote config: " + eventName + " is ignored by remote config");
                        return true;
                    }
                }
            } catch (JSONException e) {
                ZALog.printStackTrace(e);
            }
        }
        return false;
    }

    /**
     * 将 json 格式的字符串转成 ZallDataSDKRemoteConfig 对象，并处理默认值
     *
     * @param config String
     * @return ZallDataSDKRemoteConfig
     */
    protected ZallDataSDKRemoteConfig toSDKRemoteConfig(String config) {
        ZallDataSDKRemoteConfig sdkRemoteConfig = new ZallDataSDKRemoteConfig();
        try {
            if (!TextUtils.isEmpty(config)) {
                JSONObject jsonObject = new JSONObject(config);
                sdkRemoteConfig.setOldVersion(jsonObject.optString("v"));

                String configs = jsonObject.optString("configs");
                SecreteKey secreteKey = new SecreteKey("", -1, "", "");
                if (!TextUtils.isEmpty(configs)) {
                    JSONObject configObject = new JSONObject(configs);
                    sdkRemoteConfig.setDisableDebugMode(configObject.optBoolean("disableDebugMode", false));
                    sdkRemoteConfig.setDisableSDK(configObject.optBoolean("disableSDK", false));
                    sdkRemoteConfig.setAutoTrackMode(configObject.optInt("autoTrackMode", -1));
                    sdkRemoteConfig.setEventBlacklist(configObject.optJSONArray("event_blacklist"));
                    sdkRemoteConfig.setNewVersion(configObject.optString("nv", ""));
                    sdkRemoteConfig.setEffectMode(configObject.optInt("effect_mode", 0));
                    if (mZAConfigOptions.getEncryptors() != null && !mZAConfigOptions.getEncryptors().isEmpty()) {
                        JSONObject keyObject = configObject.optJSONObject("key_v2");
                        if (keyObject != null) {
                            String[] types = keyObject.optString("type").split("\\+");
                            if (types.length == 2) {
                                String asymmetricType = types[0];
                                String symmetricType = types[1];
                                for (ZAEncryptListener encryptListener : mZAConfigOptions.getEncryptors()) {
                                    if (asymmetricType.equals(encryptListener.asymmetricEncryptType())
                                            && symmetricType.equals(encryptListener.symmetricEncryptType())) {
                                        secreteKey.key = keyObject.optString("public_key");
                                        secreteKey.version = keyObject.optInt("pkv");
                                        secreteKey.asymmetricEncryptType = asymmetricType;
                                        secreteKey.symmetricEncryptType = symmetricType;
                                    }
                                }
                            }
                        }
                        if (TextUtils.isEmpty(secreteKey.key)) {
                            parseSecreteKey(configObject.optJSONObject("key"), secreteKey);
                        }
                        sdkRemoteConfig.setSecretKey(secreteKey);
                    }
                } else {
                    //默认配置
                    sdkRemoteConfig.setDisableDebugMode(false);
                    sdkRemoteConfig.setDisableSDK(false);
                    sdkRemoteConfig.setAutoTrackMode(-1);
                    sdkRemoteConfig.setSecretKey(secreteKey);
                    sdkRemoteConfig.setEventBlacklist(new JSONArray());
                    sdkRemoteConfig.setNewVersion("");
                    sdkRemoteConfig.setEffectMode(0);
                }
                return sdkRemoteConfig;
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return sdkRemoteConfig;
    }

    private void parseSecreteKey(JSONObject keyObject, SecreteKey secreteKey) {
        if (keyObject != null) {
            try {
                if (keyObject.has("key_ec") && ZallDataEncrypt.isECEncrypt()) {
                    String key_ec = keyObject.optString("key_ec");
                    if (!TextUtils.isEmpty(key_ec)) {
                        keyObject = new JSONObject(key_ec);
                    }
                }

                secreteKey.key = keyObject.optString("public_key");
                secreteKey.symmetricEncryptType = "AES";
                if (keyObject.has("type")) {
                    String type = keyObject.optString("type");
                    secreteKey.key = type + ":" + secreteKey.key;
                    secreteKey.asymmetricEncryptType = type;
                } else {
                    secreteKey.asymmetricEncryptType = "RSA";
                }
                secreteKey.version = keyObject.optInt("pkv");
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    /**
     * 全埋点类型是否被在线控制忽略
     *
     * @param autoTrackEventType 全埋点类型
     * @return true 表示该类型被忽略，false 表示不被忽略，null 表示使用本地代码配置
     */
    public Boolean isAutoTrackEventTypeIgnored(int autoTrackEventType) {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() != ZallDataSDKRemoteConfig.REMOTE_EVENT_TYPE_NO_USE) {
                if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                    return true;
                }
                return mSDKRemoteConfig.isAutoTrackEventTypeIgnored(autoTrackEventType);
            }
        }
        return null;
    }

    public static boolean isSDKDisabledByRemote() {
        if (mSDKRemoteConfig == null) {
            return false;
        }
        return mSDKRemoteConfig.isDisableSDK();
    }

    /**
     * 全埋点是否被在线控制禁止
     *
     * @return false 表示全部全埋点被禁止，true 表示部分未被禁止，null 表示使用本地代码配置
     */
    public Boolean isAutoTrackEnabled() {
        if (mSDKRemoteConfig != null) {
            if (mSDKRemoteConfig.getAutoTrackMode() == 0) {
                ZALog.i(TAG, "remote config: AutoTrackMode is closing by remote config");
                return false;
            } else if (mSDKRemoteConfig.getAutoTrackMode() > 0) {
                return true;
            }
        }
        return null;
    }

    /**
     * 获取远程配置的 Url
     *
     * @param enableConfigV 是否在 Url 中携带 v 和 ve 参数，false 表示不携带
     * @return 远程配置的 Url
     */
    protected String buildRemoteUrl(boolean enableConfigV) {
        String remoteUrl = null;
        boolean configV = enableConfigV;
        String serverUlr = mZallDataAPI.getServerUrl();
        String configOptionsRemoteUrl = null;
        if (mZAConfigOptions != null) {
            configOptionsRemoteUrl = mZAConfigOptions.mRemoteConfigUrl;
        }

        if (!TextUtils.isEmpty(configOptionsRemoteUrl)
                && Patterns.WEB_URL.matcher(configOptionsRemoteUrl).matches()) {
            remoteUrl = configOptionsRemoteUrl;
            ZALog.i(TAG, "ZAConfigOptions remote url is " + remoteUrl);
        } else if (!TextUtils.isEmpty(serverUlr) && Patterns.WEB_URL.matcher(serverUlr).matches()) {
            int pathPrefix = serverUlr.lastIndexOf("/");
            if (pathPrefix != -1) {
                remoteUrl = serverUlr.substring(0, pathPrefix);
                remoteUrl = remoteUrl + "/config/Android.conf";
            }
            ZALog.i(TAG, "ZallDataAPI remote url is " + remoteUrl);
        } else {
            ZALog.i(TAG, String.format(Locale.CHINA, "ServerUlr: %s, ZAConfigOptions remote url: %s",
                    serverUlr, configOptionsRemoteUrl));
            ZALog.i(TAG, "Remote config url verification failed");
            return null;
        }

        //再次检查是否应该在请求中带 v，比如在禁止分散请求的情况下，SDK 升级了或者公钥为空，此时应该不带 v
        if (configV && (ZallDataUtils.checkVersionIsNew(mContext, mZallDataAPI.getSDKVersion()) ||
                (mZallDataEncrypt != null && mZallDataEncrypt.isPublicSecretKeyNull()))) {
            configV = false;
        }
        Uri configUri = Uri.parse(remoteUrl);
        Uri.Builder builder = configUri.buildUpon();
        if (!TextUtils.isEmpty(remoteUrl) && configV) {
            String oldVersion = null;
            String newVersion = null;
            ZallDataSDKRemoteConfig SDKRemoteConfig = mSDKRemoteConfig;
            if (SDKRemoteConfig != null) {
                oldVersion = SDKRemoteConfig.getOldVersion();
                newVersion = SDKRemoteConfig.getNewVersion();
                ZALog.i(TAG, "The current config: " + SDKRemoteConfig.toString());
            }
            // remoteUrl 中如果存在 v，则不追加参数。nv、app_id、project 都是如此
            if (!TextUtils.isEmpty(oldVersion) && TextUtils.isEmpty(configUri.getQueryParameter("v"))) {
                builder.appendQueryParameter("v", oldVersion);
            }
            if (!TextUtils.isEmpty(newVersion) && TextUtils.isEmpty(configUri.getQueryParameter("nv"))) {
                builder.appendQueryParameter("nv", newVersion);
            }
        }
        if (!TextUtils.isEmpty(serverUlr) && TextUtils.isEmpty(configUri.getQueryParameter("project"))) {
            Uri uri = Uri.parse(serverUlr);
            String project = uri.getQueryParameter("project");
            if (!TextUtils.isEmpty(project)) {
                builder.appendQueryParameter("project", project);
            }
        }
        if (TextUtils.isEmpty(configUri.getQueryParameter("app_id"))) {
            String appId = AppInfoUtils.getProcessName(mContext);
            builder.appendQueryParameter("app_id", appId);
        }
        builder.build();
        remoteUrl = builder.toString();
        ZALog.i(TAG, "Android remote config url is " + remoteUrl);
        return remoteUrl;
    }

    /**
     * 子线程中请求网络
     *
     * @param enableConfigV 是否携带版本号
     * @param callback 请求回调接口
     */
    protected void requestRemoteConfig(boolean enableConfigV, HttpCallback.StringCallback callback) {
        try {
            String configUrl = buildRemoteUrl(enableConfigV);
            if (TextUtils.isEmpty(configUrl)) return;
            new RequestHelper.Builder(HttpMethod.GET, configUrl)
                    .callback(callback)
                    .execute();
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    public enum RandomTimeType {
        RandomTimeTypeWrite, // 创建分散请求时间
        RandomTimeTypeClean, // 移除分散请求时间
        RandomTimeTypeNone    // 不处理分散请求时间
    }
}
