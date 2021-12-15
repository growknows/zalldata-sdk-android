/*
 * Created by guo on 2021/3/2.
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

package com.zalldata.zall.android.sdk.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

import com.zalldata.zall.android.sdk.ZAConfigOptions;
import com.zalldata.zall.android.sdk.ZALog;
import com.zalldata.zall.android.sdk.ZallDataAPI;
import com.zalldata.zall.android.sdk.ZallDataAPIEmptyImplementation;
import com.zalldata.zall.android.sdk.ZallDataAutoTrackHelper;
import com.zalldata.zall.android.sdk.ServerUrl;
import com.zalldata.zall.android.sdk.advert.utils.ChannelUtils;
import com.zalldata.zall.android.sdk.dialog.ZallDataDialogUtils;
import com.zalldata.zall.android.sdk.remote.BaseZallDataSDKRemoteManager;
import com.zalldata.zall.android.sdk.remote.ZallDataRemoteManagerDebug;

public class ZASchemeHelper {

    private final static String TAG = "ZA.ZASchemeUtil";

    public static void handleSchemeUrl(Activity activity, Intent intent) {
        if (ZallDataAPI.isSDKDisabled()) {
            ZALog.i(TAG, "SDK is disabled,scan code function has been turned off");
            return;
        }
        if (ZallDataAPI.sharedInstance() instanceof ZallDataAPIEmptyImplementation) {
            ZALog.i(TAG, "SDK is not init");
            return;
        }
        try {
            Uri uri = null;
            if (activity != null && intent != null) {
                uri = intent.getData();
            }
            if (uri != null) {
                ZallDataAPI zallDataAPI = ZallDataAPI.sharedInstance();
                String host = uri.getHost();
                if ("heatmap".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    if (checkProjectIsValid(postUrl)) {
                        ZallDataDialogUtils.showOpenHeatMapDialog(activity, featureCode, postUrl);
                    } else {
                        ZallDataDialogUtils.showDialog(activity, "App 集成的项目与电脑浏览器打开的项目不同，无法进行点击分析");
                    }
                    intent.setData(null);
                } else if ("debugmode".equals(host)) {
                    String infoId = uri.getQueryParameter("info_id");
                    String locationHref = uri.getQueryParameter("sf_push_distinct_id");
                    String project = uri.getQueryParameter("project");
                    ZallDataDialogUtils.showDebugModeSelectDialog(activity, infoId, locationHref, project);
                    intent.setData(null);
                } else if ("visualized".equals(host)) {
                    String featureCode = uri.getQueryParameter("feature_code");
                    String postUrl = uri.getQueryParameter("url");
                    if (checkProjectIsValid(postUrl)) {
                        ZallDataDialogUtils.showOpenVisualizedAutoTrackDialog(activity, featureCode, postUrl);
                    } else {
                        ZallDataDialogUtils.showDialog(activity, "App 集成的项目与电脑浏览器打开的项目不同，无法进行可视化全埋点。");
                    }
                    intent.setData(null);
                } else if ("popupwindow".equals(host)) {
                    ZallDataDialogUtils.showPopupWindowDialog(activity, uri);
                    intent.setData(null);
                } else if ("encrypt".equals(host)) {
                    String version = uri.getQueryParameter("v");
                    String key = Uri.decode(uri.getQueryParameter("key"));
                    String symmetricEncryptType = Uri.decode(uri.getQueryParameter("symmetricEncryptType"));
                    String asymmetricEncryptType = Uri.decode(uri.getQueryParameter("asymmetricEncryptType"));
                    ZALog.d(TAG, "Encrypt, version = " + version
                            + ", key = " + key
                            + ", symmetricEncryptType = " + symmetricEncryptType
                            + ", asymmetricEncryptType = " + asymmetricEncryptType);
                    String tip;
                    if (TextUtils.isEmpty(version) || TextUtils.isEmpty(key)) {
                        tip = "密钥验证不通过，所选密钥无效";
                    } else if (zallDataAPI.getZallDataEncrypt() != null) {
                        tip = zallDataAPI.getZallDataEncrypt().checkPublicSecretKey(version, key, symmetricEncryptType, asymmetricEncryptType);
                    } else {
                        tip = "当前 App 未开启加密，请开启加密后再试";
                    }
                    Toast.makeText(activity, tip, Toast.LENGTH_LONG).show();
                    ZallDataDialogUtils.startLaunchActivity(activity);
                    intent.setData(null);
                } else if ("channeldebug".equals(host)) {
                    if (ChannelUtils.hasUtmByMetaData(activity)) {
                        ZallDataDialogUtils.showDialog(activity, "当前为渠道包，无法使用联调诊断工具");
                        return;
                    }

                    String monitorId = uri.getQueryParameter("monitor_id");
                    if (TextUtils.isEmpty(monitorId)) {
                        ZallDataDialogUtils.startLaunchActivity(activity);
                        return;
                    }
                    String url = ZallDataAPI.sharedInstance().getServerUrl();
                    if (TextUtils.isEmpty(url)) {
                        ZallDataDialogUtils.showDialog(activity, "数据接收地址错误，无法使用联调诊断工具");
                        return;
                    }
                    ServerUrl serverUrl = new ServerUrl(url);
                    String projectName = uri.getQueryParameter("project_name");
                    if (serverUrl.getProject().equals(projectName)) {
                        String projectId = uri.getQueryParameter("project_id");
                        String accountId = uri.getQueryParameter("account_id");
                        String isReLink = uri.getQueryParameter("is_relink");
                        if ("1".equals(isReLink)) {//续连标识 1 :续连
                            String deviceCode = uri.getQueryParameter("device_code");
                            if (ChannelUtils.checkDeviceInfo(activity, deviceCode)) {//比较设备信息是否匹配
                                ZallDataAutoTrackHelper.showChannelDebugActiveDialog(activity);
                            } else {
                                ZallDataDialogUtils.showDialog(activity, "无法重连，请检查是否更换了联调手机");
                            }
                        } else {
                            ZallDataDialogUtils.showChannelDebugDialog(activity, serverUrl.getBaseUrl(), monitorId, projectId, accountId);
                        }
                    } else {
                        ZallDataDialogUtils.showDialog(activity, "App 集成的项目与电脑浏览器打开的项目不同，无法使用联调诊断工具");
                    }
                    intent.setData(null);
                } else if ("abtest".equals(host)) {
                    try {
                        ReflectUtil.callStaticMethod(Class.forName("com.zalldata.abtest.core.ZallABTestSchemeHandler"), "handleSchemeUrl", uri.toString());
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                    ZallDataDialogUtils.startLaunchActivity(activity);
                    intent.setData(null);
                } else if ("zalldataremoteconfig".equals(host)) {
                    // 开启日志
                    ZallDataAPI.sharedInstance().enableLog(true);
                    BaseZallDataSDKRemoteManager zallDataSDKRemoteManager = zallDataAPI.getRemoteManager();
                    // 取消重试
                    if (zallDataSDKRemoteManager != null) {
                        zallDataSDKRemoteManager.resetPullSDKConfigTimer();
                    }
                    final ZallDataRemoteManagerDebug zallDataRemoteManagerDebug =
                            new ZallDataRemoteManagerDebug(zallDataAPI);
                    // 替换为 ZallDataRemoteManagerDebug 对象
                    zallDataAPI.setRemoteManager(zallDataRemoteManagerDebug);
                    // 验证远程配置
                    ZALog.i(TAG, "Start debugging remote config");
                    zallDataRemoteManagerDebug.checkRemoteConfig(uri, activity);
                    intent.setData(null);
                } else if ("assistant".equals(host)) {
                    ZAConfigOptions configOptions = ZallDataAPI.getConfigOptions();
                    if (configOptions != null && configOptions.mDisableDebugAssistant) {
                        return;
                    }
                    String service = uri.getQueryParameter("service");
                    if ("pairingCode".equals(service)) {
                        ZallDataDialogUtils.showPairingCodeInputDialog(activity);
                    }
                } else {
                    ZallDataDialogUtils.startLaunchActivity(activity);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private static boolean checkProjectIsValid(String url) {
        String serverUrl = ZallDataAPI.sharedInstance().getServerUrl();
        String sdkProject = null, serverProject = null;
        if (!TextUtils.isEmpty(url)) {
            Uri schemeUri = Uri.parse(url);
            if (schemeUri != null) {
                sdkProject = schemeUri.getQueryParameter("project");
            }
        }
        if (!TextUtils.isEmpty(serverUrl)) {
            Uri serverUri = Uri.parse(serverUrl);
            if (serverUri != null) {
                serverProject = serverUri.getQueryParameter("project");
            }
        }
        return !TextUtils.isEmpty(sdkProject) && !TextUtils.isEmpty(serverProject) && TextUtils.equals(sdkProject, serverProject);
    }
}
