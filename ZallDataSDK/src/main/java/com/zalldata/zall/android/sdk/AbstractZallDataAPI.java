/*
 * Created by guo on 2020/1/3.
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

package com.zalldata.zall.android.sdk;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.zalldata.zall.android.sdk.advert.utils.ChannelUtils;
import com.zalldata.zall.android.sdk.advert.utils.OaidHelper;
import com.zalldata.zall.android.sdk.aop.push.PushLifecycleCallbacks;
import com.zalldata.zall.android.sdk.autotrack.ActivityLifecycleCallbacks;
import com.zalldata.zall.android.sdk.autotrack.ActivityPageLeaveCallbacks;
import com.zalldata.zall.android.sdk.autotrack.FragmentPageLeaveCallbacks;
import com.zalldata.zall.android.sdk.autotrack.FragmentViewScreenCallbacks;
import com.zalldata.zall.android.sdk.autotrack.aop.FragmentTrackHelper;
import com.zalldata.zall.android.sdk.data.adapter.DbAdapter;
import com.zalldata.zall.android.sdk.data.adapter.DbParams;
import com.zalldata.zall.android.sdk.data.persistent.PersistentDistinctId;
import com.zalldata.zall.android.sdk.data.persistent.PersistentFirstDay;
import com.zalldata.zall.android.sdk.data.persistent.PersistentFirstStart;
import com.zalldata.zall.android.sdk.data.persistent.PersistentFirstTrackInstallation;
import com.zalldata.zall.android.sdk.data.persistent.PersistentFirstTrackInstallationWithCallback;
import com.zalldata.zall.android.sdk.data.persistent.PersistentLoader;
import com.zalldata.zall.android.sdk.data.persistent.PersistentSuperProperties;
import com.zalldata.zall.android.sdk.deeplink.ZallDataDeepLinkCallback;
import com.zalldata.zall.android.sdk.encrypt.ZallDataEncrypt;
import com.zalldata.zall.android.sdk.exceptions.InvalidDataException;
import com.zalldata.zall.android.sdk.internal.api.FragmentAPI;
import com.zalldata.zall.android.sdk.internal.api.IFragmentAPI;
import com.zalldata.zall.android.sdk.internal.rpc.ZallDataContentObserver;
import com.zalldata.zall.android.sdk.listener.ZAEventListener;
import com.zalldata.zall.android.sdk.listener.ZAFunctionListener;
import com.zalldata.zall.android.sdk.listener.ZAJSListener;
import com.zalldata.zall.android.sdk.remote.BaseZallDataSDKRemoteManager;
import com.zalldata.zall.android.sdk.remote.ZallDataRemoteManager;
import com.zalldata.zall.android.sdk.util.AppInfoUtils;
import com.zalldata.zall.android.sdk.util.JSONUtils;
import com.zalldata.zall.android.sdk.util.NetworkUtils;
import com.zalldata.zall.android.sdk.util.ZAContextManager;
import com.zalldata.zall.android.sdk.util.ZADataHelper;
import com.zalldata.zall.android.sdk.util.ZallDataUtils;
import com.zalldata.zall.android.sdk.util.TimeUtils;
import com.zalldata.zall.android.sdk.visual.model.ViewNode;
import com.zalldata.zall.android.sdk.visual.property.VisualPropertiesManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.zalldata.zall.android.sdk.util.ZADataHelper.assertKey;
import static com.zalldata.zall.android.sdk.util.ZADataHelper.assertPropertyTypes;
import static com.zalldata.zall.android.sdk.util.ZADataHelper.assertValue;

abstract class AbstractZallDataAPI implements IZallDataAPI {
    protected static final String TAG = "ZA.ZallDataAPI";
    // SDK版本
    static final String VERSION = BuildConfig.SDK_VERSION;
    // Maps each token to a singleton ZallDataAPI instance
    protected static final Map<Context, ZallDataAPI> sInstanceMap = new HashMap<>();
    static boolean mIsMainProcess = false;
    static boolean SHOW_DEBUG_INFO_VIEW = true;
    protected static ZallDataGPSLocation mGPSLocation;
    /* 远程配置 */
    protected static ZAConfigOptions mZAConfigOptions;
    protected ZAContextManager mZAContextManager;
    protected final Context mContext;
    protected ActivityLifecycleCallbacks mActivityLifecycleCallbacks;
    protected AnalyticsMessages mMessages;
    protected final PersistentDistinctId mDistinctId;
    protected final PersistentSuperProperties mSuperProperties;
    protected final PersistentFirstStart mFirstStart;
    protected final PersistentFirstDay mFirstDay;
    protected final PersistentFirstTrackInstallation mFirstTrackInstallation;
    protected final PersistentFirstTrackInstallationWithCallback mFirstTrackInstallationWithCallback;
    protected final Map<String, EventTimer> mTrackTimer;
    protected final Object mLoginIdLock = new Object();
    protected List<Class> mIgnoredViewTypeList = new ArrayList<>();
    /* LoginId */
    protected String mLoginId = null;
    /* ZallData 地址 */
    protected String mServerUrl;
    protected String mOriginServerUrl;
    /* SDK 配置是否初始化 */
    protected boolean mSDKConfigInit;
    /* Debug 模式选项 */
    protected ZallDataAPI.DebugMode mDebugMode = ZallDataAPI.DebugMode.DEBUG_OFF;
    /* SDK 自动采集事件 */
    protected boolean mAutoTrack;
    /* 上个页面的 Url */
    protected String mLastScreenUrl;
    /* 上个页面的 Title */
    protected String mReferrerScreenTitle;
    /* 当前页面的 Title */
    protected String mCurrentScreenTitle;
    protected JSONObject mLastScreenTrackProperties;
    /* 是否请求网络 */
    protected boolean mEnableNetworkRequest = true;
    protected boolean mClearReferrerWhenAppEnd = false;
    protected boolean mDisableDefaultRemoteConfig = false;
    protected boolean mDisableTrackDeviceId = false;
    protected static boolean isChangeEnableNetworkFlag = false;
    // Session 时长
    protected int mSessionTime = 30 * 1000;
    protected List<Integer> mAutoTrackIgnoredActivities;
    protected List<Integer> mHeatMapActivities;
    protected List<Integer> mVisualizedAutoTrackActivities;
    protected String mCookie;
    protected TrackTaskManager mTrackTaskManager;
    protected TrackTaskManagerThread mTrackTaskManagerThread;
    protected ZallDataScreenOrientationDetector mOrientationDetector;
    protected ZallDataDynamicSuperProperties mDynamicSuperPropertiesCallBack;
    protected SimpleDateFormat mIsFirstDayDateFormat;
    protected ZallDataTrackEventCallBack mTrackEventCallBack;
    protected List<ZAEventListener> mEventListenerList;
    protected List<ZAFunctionListener> mFunctionListenerList;
    private CopyOnWriteArrayList<ZAJSListener> mZAJSListeners;
    protected IFragmentAPI mFragmentAPI;
    ZallDataEncrypt mZallDataEncrypt;
    protected ZallDataDeepLinkCallback mDeepLinkCallback;
    // $AppDeeplinkLaunch 是否携带设备信息
    boolean mEnableDeepLinkInstallSource = false;
    BaseZallDataSDKRemoteManager mRemoteManager;
    /**
     * 标记是否已经采集了带有插件版本号的事件
     */
    private boolean isTrackEventWithPluginVersion = false;

    public AbstractZallDataAPI(Context context, ZAConfigOptions configOptions, ZallDataAPI.DebugMode debugMode) {
        mContext = context;
        setDebugMode(debugMode);
        final String packageName = context.getApplicationContext().getPackageName();
        mAutoTrackIgnoredActivities = new ArrayList<>();
        mHeatMapActivities = new ArrayList<>();
        mVisualizedAutoTrackActivities = new ArrayList<>();
        PersistentLoader.initLoader(context);
        mDistinctId = (PersistentDistinctId) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.DISTINCT_ID);
        mSuperProperties = (PersistentSuperProperties) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.SUPER_PROPERTIES);
        mFirstStart = (PersistentFirstStart) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_START);
        mFirstTrackInstallation = (PersistentFirstTrackInstallation) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL);
        mFirstTrackInstallationWithCallback = (PersistentFirstTrackInstallationWithCallback) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_INSTALL_CALLBACK);
        mFirstDay = (PersistentFirstDay) PersistentLoader.loadPersistent(PersistentLoader.PersistentName.FIRST_DAY);
        mTrackTimer = new HashMap<>();
        mFragmentAPI = new FragmentAPI();
        try {
            mZAConfigOptions = configOptions.clone();
            mTrackTaskManager = TrackTaskManager.getInstance();
            mTrackTaskManagerThread = new TrackTaskManagerThread();
            new Thread(mTrackTaskManagerThread, ThreadNameConstants.THREAD_TASK_QUEUE).start();
            ZallDataExceptionHandler.init();
            initZAConfig(mZAConfigOptions.mServerUrl, packageName);
            mZAContextManager = new ZAContextManager(mContext, mDisableTrackDeviceId);
            mMessages = AnalyticsMessages.getInstance(mContext, (ZallDataAPI) this);
            mRemoteManager = new ZallDataRemoteManager((ZallDataAPI) this);
            //先从缓存中读取 SDKConfig
            mRemoteManager.applySDKConfigFromCache();
            // 可视化自定义属性拉取配置
            if (mZAConfigOptions.isVisualizedPropertiesEnabled()) {
                VisualPropertiesManager.getInstance().requestVisualConfig(mContext, (ZallDataAPI) this);
            }
            //打开 debug 模式，弹出提示
            if (mDebugMode != ZallDataAPI.DebugMode.DEBUG_OFF && mIsMainProcess) {
                if (SHOW_DEBUG_INFO_VIEW) {
                    if (!isSDKDisabled()) {
                        showDebugModeWarning();
                    }
                }
            }

            registerLifecycleCallbacks();
            registerObserver();
            if (!mZAConfigOptions.isDisableSDK()) {
                delayInitTask();
            }
            if (ZALog.isLogEnabled()) {
                ZALog.i(TAG, String.format(Locale.CHINA, "Initialized the instance of Zall Data SDK with server"
                        + " url '%s', flush interval %d ms, debugMode: %s", mServerUrl, mZAConfigOptions.mFlushInterval, debugMode));
            }
            mLoginId = DbAdapter.getInstance().getLoginId();
            ZallDataUtils.initUniAppStatus();
        } catch (Throwable ex) {
            ZALog.d(TAG, ex.getMessage());
        }
    }

    protected AbstractZallDataAPI() {
        mContext = null;
        mMessages = null;
        mDistinctId = null;
        mSuperProperties = null;
        mFirstStart = null;
        mFirstDay = null;
        mFirstTrackInstallation = null;
        mFirstTrackInstallationWithCallback = null;
        mTrackTimer = null;
        mZallDataEncrypt = null;
    }

    /**
     * 延迟初始化处理逻辑
     *
     * @param activity 延迟初始化 Activity 补充执行
     */
    protected void delayExecution(Activity activity) {
        if (mActivityLifecycleCallbacks != null) {
            mActivityLifecycleCallbacks.onActivityCreated(activity, null);   //延迟初始化处理唤起逻辑
            AppStateManager.getInstance().onActivityCreated(activity, null); //可视化获取页面信息
            mActivityLifecycleCallbacks.onActivityStarted(activity);                 //延迟初始化补发应用启动逻辑
        }
        if (ZALog.isLogEnabled()) {
            ZALog.i(TAG, "SDK init success by：" + activity.getClass().getName());
        }
    }

    /**
     * 返回采集控制是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisabledByRemote() {
        boolean isSDKDisabled = ZallDataRemoteManager.isSDKDisabledByRemote();
        if (isSDKDisabled) {
            ZALog.i(TAG, "remote config: SDK is disabled");
        }
        return isSDKDisabled;
    }

    /**
     * 返回本地是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    private static boolean isSDKDisableByLocal() {
        if (mZAConfigOptions == null) {
            ZALog.i(TAG, "ZAConfigOptions is null");
            return true;
        }
        return mZAConfigOptions.isDisableSDK;
    }

    /**
     * 返回是否关闭了 SDK
     *
     * @return true：关闭；false：没有关闭
     */
    public static boolean isSDKDisabled() {
        return isSDKDisableByLocal() || isSDKDisabledByRemote();
    }

    /**
     * SDK 事件回调监听，目前用于弹窗业务
     *
     * @param eventListener 事件监听
     */
    public void addEventListener(ZAEventListener eventListener) {
        try {
            if (this.mEventListenerList == null) {
                this.mEventListenerList = new ArrayList<>();
            }
            this.mEventListenerList.add(eventListener);
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param eventListener 事件监听
     */
    public void removeEventListener(ZAEventListener eventListener) {
        try {
            if (mEventListenerList != null && mEventListenerList.contains(eventListener)) {
                this.mEventListenerList.remove(eventListener);
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 监听 JS 消息
     *
     * @param listener JS 监听
     */
    public void addZAJSListener(final ZAJSListener listener) {
        try {
            if (mZAJSListeners == null) {
                mZAJSListeners = new CopyOnWriteArrayList<>();
            }
            if (!mZAJSListeners.contains(listener)) {
                mZAJSListeners.add(listener);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 移除 JS 消息
     *
     * @param listener JS 监听
     */
    public void removeZAJSListener(final ZAJSListener listener) {
        try {
            if (mZAJSListeners != null && mZAJSListeners.contains(listener)) {
                this.mZAJSListeners.remove(listener);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    void handleJsMessage(WeakReference<View> view, final String message) {
        if (mZAJSListeners != null && mZAJSListeners.size() > 0) {
            for (final ZAJSListener listener : mZAJSListeners) {
                try {
                    if (listener != null) {
                        listener.onReceiveJSMessage(view, message);
                    }
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        }
    }

    /**
     * SDK 函数回调监听
     *
     * @param functionListener 事件监听
     */
    public void addFunctionListener(ZAFunctionListener functionListener) {
        try {
            if (this.mFunctionListenerList == null) {
                mFunctionListenerList = new ArrayList<>();
            }
            if (functionListener != null && !mFunctionListenerList.contains(functionListener)) {
                mFunctionListenerList.add(functionListener);
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 移除 SDK 事件回调监听
     *
     * @param functionListener 事件监听
     */
    public void removeFunctionListener(ZAFunctionListener functionListener) {
        try {
            if (this.mFunctionListenerList != null && functionListener != null) {
                this.mFunctionListenerList.remove(functionListener);
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    public static ZAConfigOptions getConfigOptions() {
        return mZAConfigOptions;
    }

    public Context getContext() {
        return mContext;
    }

    boolean isSaveDeepLinkInfo() {
        return mZAConfigOptions.mEnableSaveDeepLinkInfo;
    }

    public ZallDataDeepLinkCallback getDeepLinkCallback() {
        return mDeepLinkCallback;
    }

    boolean _trackEventFromH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return false;
            }
            JSONObject eventObject = new JSONObject(eventInfo);

            String serverUrl = eventObject.optString("server_url");
            if (!TextUtils.isEmpty(serverUrl)) {
                if (!(new ServerUrl(serverUrl).check(new ServerUrl(mServerUrl)))) {
                    return false;
                }
                trackEventFromH5(eventInfo);
                return true;
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     */
    public void trackInternal(final String eventName, final JSONObject properties) {
        trackInternal(eventName, properties, null);
    }

    /**
     * SDK 内部用来调用触发事件
     *
     * @param eventName 事件名称
     * @param properties 事件属性
     * @param viewNode ViewTree 中的 View 节点
     */
    public void trackInternal(final String eventName, final JSONObject properties, final ViewNode viewNode) {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (viewNode != null && ZallDataAPI.getConfigOptions().isVisualizedPropertiesEnabled()) {
                        VisualPropertiesManager.getInstance().mergeVisualProperties(VisualPropertiesManager.VisualEventType.APP_CLICK, properties, viewNode);
                    }
                    trackEvent(EventType.TRACK, eventName, properties, null);
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        });
    }

    public ZallDataAPI.DebugMode getDebugMode() {
        return mDebugMode;
    }

    public void setDebugMode(ZallDataAPI.DebugMode debugMode) {
        mDebugMode = debugMode;
        if (debugMode == ZallDataAPI.DebugMode.DEBUG_OFF) {
            enableLog(false);
            ZALog.setDebug(false);
            mServerUrl = mOriginServerUrl;
        } else {
            enableLog(true);
            ZALog.setDebug(true);
            setServerUrl(mOriginServerUrl);
        }
    }

    void enableAutoTrack(int autoTrackEventType) {
        try {
            if (autoTrackEventType <= 0 || autoTrackEventType > 15) {
                return;
            }
            this.mAutoTrack = true;
            mZAConfigOptions.setAutoTrackEventType(mZAConfigOptions.mAutoTrackEventType | autoTrackEventType);
        } catch (Exception e) {
            com.zalldata.zall.android.sdk.ZALog.printStackTrace(e);
        }
    }

    public BaseZallDataSDKRemoteManager getRemoteManager() {
        return mRemoteManager;
    }

    public void setRemoteManager(BaseZallDataSDKRemoteManager remoteManager) {
        this.mRemoteManager = remoteManager;
    }

    public ZallDataEncrypt getZallDataEncrypt() {
        return mZallDataEncrypt;
    }

    public boolean isDisableDefaultRemoteConfig() {
        return mDisableDefaultRemoteConfig;
    }

    /**
     * App 从后台恢复，遍历 mTrackTimer
     * startTime = System.currentTimeMillis()
     */
    public void appBecomeActive() {
        synchronized (mTrackTimer) {
            try {
                for (Map.Entry<String, EventTimer> entry : mTrackTimer.entrySet()) {
                    if (entry != null) {
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null) {
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                ZALog.i(TAG, "appBecomeActive error:" + e.getMessage());
            }
        }
    }

    /**
     * App 进入后台，遍历 mTrackTimer
     * eventAccumulatedDuration =
     * eventAccumulatedDuration + System.currentTimeMillis() - startTime - SessionIntervalTime
     */
    public void appEnterBackground() {
        synchronized (mTrackTimer) {
            try {
                for (Map.Entry<String, EventTimer> entry : mTrackTimer.entrySet()) {
                    if (entry != null) {
                        if ("$AppEnd".equals(entry.getKey())) {
                            continue;
                        }
                        EventTimer eventTimer = (EventTimer) entry.getValue();
                        if (eventTimer != null && !eventTimer.isPaused()) {
                            long eventAccumulatedDuration =
                                    eventTimer.getEventAccumulatedDuration() + SystemClock.elapsedRealtime() - eventTimer.getStartTime() - getSessionIntervalTime();
                            eventTimer.setEventAccumulatedDuration(eventAccumulatedDuration);
                            eventTimer.setStartTime(SystemClock.elapsedRealtime());
                        }
                    }
                }
            } catch (Exception e) {
                ZALog.i(TAG, "appEnterBackground error:" + e.getMessage());
            }
        }
    }

    void trackChannelDebugInstallation() {
        final JSONObject _properties = new JSONObject();
        addTimeProperty(_properties);
        transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    _properties.put("$ios_install_source", ChannelUtils.getDeviceInfo(mContext,
                            mZAContextManager.getAndroidId(), OaidHelper.getOAID(mContext)));
                    // 先发送 track
                    trackEvent(EventType.TRACK, "$ChannelDebugInstall", _properties, null);

                    // 再发送 profile_set_once 或者 profile_set
                    JSONObject profileProperties = new JSONObject();
                    ZallDataUtils.mergeJSONObject(_properties, profileProperties);
                    profileProperties.put("$first_visit_time", new java.util.Date());
                    trackEvent(EventType.PROFILE_SET_ONCE, null, profileProperties, null);
                    flush();
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        });
    }

    /**
     * SDK 内部调用方法
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    public void trackAutoEvent(final String eventName, final JSONObject properties) {
        trackAutoEvent(eventName, properties, null);
    }

    /**
     * SDK 全埋点调用方法，支持可视化自定义属性
     *
     * @param eventName 事件名
     * @param properties 事件属性
     */
    void trackAutoEvent(final String eventName, final JSONObject properties, final ViewNode viewNode) {
        //添加 $lib_method 属性
        JSONObject eventProperties = ZADataHelper.appendLibMethodAutoTrack(properties);
        trackInternal(eventName, eventProperties, viewNode);
    }

    public ZAContextManager getZAContextManager() {
        return mZAContextManager;
    }

    void registerNetworkListener() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.registerNetworkListener(mContext);
            }
        });
    }

    void unregisterNetworkListener() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                NetworkUtils.unregisterNetworkListener(mContext);
            }
        });
    }

    protected void addTimeProperty(JSONObject jsonObject) {
        if (!jsonObject.has("$time")) {
            try {
                jsonObject.put("$time", new Date(System.currentTimeMillis()));
            } catch (JSONException e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    protected boolean isFirstDay(long eventTime) {
        String firstDay = mFirstDay.get();
        if (firstDay == null) {
            return true;
        }
        try {
            if (mIsFirstDayDateFormat == null) {
                mIsFirstDayDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            }
            String current = mIsFirstDayDateFormat.format(eventTime);
            return firstDay.equals(current);
        } catch (Exception e) {
            com.zalldata.zall.android.sdk.ZALog.printStackTrace(e);
        }
        return true;
    }

    protected void trackItemEvent(String itemType, String itemId, String eventType, long time, JSONObject properties) {
        try {
            assertKey(itemType);
            assertValue(itemId);
            assertPropertyTypes(properties);

            // 禁用采集事件时，先计算基本信息存储到缓存中
            if (!mZAConfigOptions.isDataCollectEnable) {
                transformItemTaskQueue(itemType, itemId, eventType, time, properties);
                return;
            }

            String eventProject = null;
            if (properties != null && properties.has("$project")) {
                eventProject = (String) properties.get("$project");
                properties.remove("$project");
            }

            JSONObject libProperties = new JSONObject();
            libProperties.put("$lib", "Android");
            libProperties.put("$lib_version", VERSION);
            libProperties.put("$lib_method", "code");
            mZAContextManager.addKeyIfExist(libProperties, "$app_version");

            JSONObject superProperties = mSuperProperties.get();
            if (superProperties != null) {
                if (superProperties.has("$app_version")) {
                    libProperties.put("$app_version", superProperties.get("$app_version"));
                }
            }

            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                String libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
                if (!TextUtils.isEmpty(libDetail)) {
                    libProperties.put("$lib_detail", libDetail);
                }
            }

            JSONObject eventProperties = new JSONObject();
            eventProperties.put("item_type", itemType);
            eventProperties.put("item_id", itemId);
            eventProperties.put("type", eventType);
            eventProperties.put("time", time);
            eventProperties.put("properties", TimeUtils.formatDate(properties));
            eventProperties.put("lib", libProperties);

            if (!TextUtils.isEmpty(eventProject)) {
                eventProperties.put("project", eventProject);
            }
            mMessages.enqueueEventMessage(eventType, eventProperties);
            ZALog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventProperties.toString()));
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    protected void trackEvent(EventType eventType, String eventName, JSONObject properties, String
            originalDistinctId) {
        trackEvent(eventType, eventName, properties, null, getDistinctId(), getLoginId(), originalDistinctId);
    }

    protected void trackEvent(final EventType eventType, String eventName, final JSONObject properties, JSONObject dynamicProperty, String
            distinctId, String loginId, String originalDistinctId) {
        try {
            EventTimer eventTimer = null;
            if (!TextUtils.isEmpty(eventName)) {
                synchronized (mTrackTimer) {
                    eventTimer = mTrackTimer.get(eventName);
                    mTrackTimer.remove(eventName);
                }

                if (eventName.endsWith("_ZATimer") && eventName.length() > 45) {// Timer 计时交叉计算拼接的字符串长度 45
                    eventName = eventName.substring(0, eventName.length() - 45);
                }
            }

            if (eventType.isTrack()) {
                assertKey(eventName);
                //如果在线控制禁止了事件，则不触发
                if (mRemoteManager != null && mRemoteManager.ignoreEvent(eventName)) {
                    return;
                }
            }
            assertPropertyTypes(properties);

            try {
                JSONObject sendProperties;

                if (eventType.isTrack()) {
                    Map<String, Object> deviceInfo = mZAContextManager.getDeviceInfo();
                    if (deviceInfo != null) {
                        sendProperties = new JSONObject(deviceInfo);
                    } else {
                        sendProperties = new JSONObject();
                    }
                    //之前可能会因为没有权限无法获取运营商信息，检测再次获取
                    getCarrier(sendProperties);
                    if (!"$AppEnd".equals(eventName) && !"$AppDeeplinkLaunch".equals(eventName)) {
                        //合并 $latest_utm 属性
                        ZallDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), sendProperties);
                    }
                    mergerDynamicAndSuperProperties(sendProperties, dynamicProperty);

                    if (mReferrerScreenTitle != null) {
                        sendProperties.put("$referrer_title", mReferrerScreenTitle);
                    }

                    // 当前网络状况
                    String networkType = NetworkUtils.networkType(mContext);
                    sendProperties.put("$wifi", "WIFI".equals(networkType));
                    sendProperties.put("$network_type", networkType);

                    // GPS
                    try {
                        if (mGPSLocation != null) {
                            mGPSLocation.toJSON(sendProperties);
                        }
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }

                    // 屏幕方向
                    try {
                        String screenOrientation = getScreenOrientation();
                        if (!TextUtils.isEmpty(screenOrientation)) {
                            sendProperties.put("$screen_orientation", screenOrientation);
                        }
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                } else if (eventType.isProfile()) {
                    sendProperties = new JSONObject();
                } else {
                    return;
                }

                // 禁用采集事件时，先计算基本信息存储到缓存中
                if (!mZAConfigOptions.isDataCollectEnable) {
                    if (ZALog.isLogEnabled()) {
                        ZALog.i(TAG, "track event, isDataCollectEnable = false, eventName = " + eventName + ",property = " + JSONUtils.formatJson(sendProperties.toString()));
                    }
                    transformEventTaskQueue(eventType, eventName, properties, sendProperties, distinctId, loginId, originalDistinctId, eventTimer);
                    return;
                }
                trackEventInternal(eventType, eventName, properties, sendProperties, distinctId, loginId, originalDistinctId, eventTimer);
            } catch (JSONException e) {
                throw new InvalidDataException("Unexpected property");
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 处理 H5 打通的事件
     *
     * @param eventInfo 事件信息
     */
    protected void trackEventH5(String eventInfo) {
        try {
            if (TextUtils.isEmpty(eventInfo)) {
                return;
            }

            // 禁用采集事件时，先计算基本信息存储到缓存中
            if (!mZAConfigOptions.isDataCollectEnable) {
                transformH5TaskQueue(eventInfo);
                return;
            }

            JSONObject eventObject = new JSONObject(eventInfo);
            eventObject.put("_hybrid_h5", true);
            String type = eventObject.getString("type");
            EventType eventType = EventType.valueOf(type.toUpperCase(Locale.getDefault()));

            String distinctIdKey = "distinct_id";
            if (eventType == EventType.TRACK_SIGNUP) {
                eventObject.put("original_id", getAnonymousId());
            } else if (!TextUtils.isEmpty(getLoginId())) {
                eventObject.put(distinctIdKey, getLoginId());
            } else {
                eventObject.put(distinctIdKey, getAnonymousId());
            }
            eventObject.put("anonymous_id", getAnonymousId());
            long eventTime = System.currentTimeMillis();
            eventObject.put("time", eventTime);

            try {
                SecureRandom secureRandom = new SecureRandom();
                eventObject.put("_track_id", secureRandom.nextInt());
            } catch (Exception e) {
                //ignore
            }

            JSONObject propertiesObject = eventObject.optJSONObject("properties");
            // 校验 H5 属性
            assertPropertyTypes(propertiesObject);
            if (propertiesObject == null) {
                propertiesObject = new JSONObject();
            }

            JSONObject libObject = eventObject.optJSONObject("lib");
            if (libObject != null) {
                mZAContextManager.addKeyIfExist(libObject, "$app_version");
                //update lib $app_version from super properties
                JSONObject superProperties = mSuperProperties.get();
                if (superProperties != null) {
                    if (superProperties.has("$app_version")) {
                        libObject.put("$app_version", superProperties.get("$app_version"));
                    }
                }
            }

            if (eventType.isTrack()) {
                Map<String, Object> deviceInfo = mZAContextManager.getDeviceInfo();
                if (deviceInfo != null) {
                    for (Map.Entry<String, Object> entry : deviceInfo.entrySet()) {
                        String key = entry.getKey();
                        if (!TextUtils.isEmpty(key)) {
                            if ("$lib".equals(key) || "$lib_version".equals(key)) {
                                continue;
                            }
                            propertiesObject.put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                //之前可能会因为没有权限无法获取运营商信息，检测再次获取
                getCarrier(propertiesObject);
                // 当前网络状况
                String networkType = NetworkUtils.networkType(mContext);
                propertiesObject.put("$wifi", "WIFI".equals(networkType));
                propertiesObject.put("$network_type", networkType);

                // SuperProperties
                mergerDynamicAndSuperProperties(propertiesObject, getDynamicProperty());

                //是否首日访问
                if (eventType.isTrack()) {
                    propertiesObject.put("$is_first_day", isFirstDay(eventTime));
                }
                ZallDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), propertiesObject);
            }

            if (eventObject.has("_nocache")) {
                eventObject.remove("_nocache");
            }

            if (eventObject.has("server_url")) {
                eventObject.remove("server_url");
            }

            if (eventObject.has("_flush_time")) {
                eventObject.remove("_flush_time");
            }

            if (propertiesObject.has("$project")) {
                eventObject.put("project", propertiesObject.optString("$project"));
                propertiesObject.remove("$project");
            }

            if (propertiesObject.has("$token")) {
                eventObject.put("token", propertiesObject.optString("$token"));
                propertiesObject.remove("$token");
            }

            if (propertiesObject.has("$time")) {
                try {
                    long time = propertiesObject.getLong("$time");
                    if (TimeUtils.isDateValid(time)) {
                        eventObject.put("time", time);
                    }
                } catch (Exception ex) {
                    ZALog.printStackTrace(ex);
                }
                propertiesObject.remove("$time");
            }

            String eventName = eventObject.optString("event");
            if (eventType.isTrack()) {
                // 校验 H5 事件名称
                assertKey(eventName);
                boolean enterDb = isEnterDb(eventName, propertiesObject);
                if (!enterDb) {
                    ZALog.d(TAG, eventName + " event can not enter database");
                    return;
                }

                if (!isTrackEventWithPluginVersion && !propertiesObject.has("$lib_plugin_version")) {
                    JSONArray libPluginVersion = getPluginVersion();
                    if (libPluginVersion == null) {
                        isTrackEventWithPluginVersion = true;
                    } else {
                        try {
                            propertiesObject.put("$lib_plugin_version", libPluginVersion);
                            isTrackEventWithPluginVersion = true;
                        } catch (Exception e) {
                            ZALog.printStackTrace(e);
                        }
                    }
                }
            }
            eventObject.put("properties", propertiesObject);

            if (eventType == EventType.TRACK_SIGNUP) {
                String loginId = eventObject.getString("distinct_id");
                synchronized (mLoginIdLock) {
                    if (!loginId.equals(DbAdapter.getInstance().getLoginId()) && !loginId.equals(getAnonymousId())) {
                        mLoginId = loginId;
                        DbAdapter.getInstance().commitLoginId(loginId);
                        eventObject.put("login_id", loginId);
                        try {
                            if (mEventListenerList != null) {
                                for (ZAEventListener eventListener : mEventListenerList) {
                                    eventListener.login();
                                }
                            }
                        } catch (Exception e) {
                            ZALog.printStackTrace(e);
                        }
                        try {
                            if (mFunctionListenerList != null) {
                                JSONObject jsonObject = new JSONObject();
                                jsonObject.put("distinctId", loginId);
                                for (ZAFunctionListener listener : mFunctionListenerList) {
                                    listener.call("login", jsonObject);
                                }
                            }
                        } catch (Exception e) {
                            ZALog.printStackTrace(e);
                        }
                        mMessages.enqueueEventMessage(type, eventObject);
                        if (ZALog.isLogEnabled()) {
                            ZALog.i(TAG, "track event:\n" + JSONUtils.formatJson(eventObject.toString()));
                        }
                    }
                }
            } else {
                if (!TextUtils.isEmpty(getLoginId())) {
                    eventObject.put("login_id", getLoginId());
                }
                try {
                    if (mEventListenerList != null && eventType.isTrack()) {
                        for (ZAEventListener eventListener : mEventListenerList) {
                            eventListener.trackEvent(eventObject);
                        }
                    }
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
                try {
                    if (mFunctionListenerList != null && eventType.isTrack()) {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("eventJSON", eventObject);
                        for (ZAFunctionListener listener : mFunctionListenerList) {
                            listener.call("trackEvent", jsonObject);
                        }
                    }
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
                mMessages.enqueueEventMessage(type, eventObject);
                if (ZALog.isLogEnabled()) {
                    ZALog.i(TAG, "track event from H5:\n" + JSONUtils.formatJson(eventObject.toString()));
                }
            }
        } catch (Exception e) {
            //ignore
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 在未同意合规时转换队列
     *
     * @param runnable 任务
     */
    public void transformTaskQueue(final Runnable runnable) {
        // 禁用采集事件时，先计算基本信息存储到缓存中
        if (!mZAConfigOptions.isDataCollectEnable) {
            mTrackTaskManager.addTrackEventTask(new Runnable() {
                @Override
                public void run() {
                    mTrackTaskManager.transformTaskQueue(runnable);
                }
            });
            return;
        }

        mTrackTaskManager.addTrackEventTask(runnable);
    }

    protected void initZAConfig(String serverURL, String packageName) {
        Bundle configBundle = AppInfoUtils.getAppInfoBundle(mContext);
        if (mZAConfigOptions == null) {
            this.mSDKConfigInit = false;
            mZAConfigOptions = new ZAConfigOptions(serverURL);
        } else {
            this.mSDKConfigInit = true;
        }

        if (mZAConfigOptions.mEnableEncrypt) {
            mZallDataEncrypt = new ZallDataEncrypt(mContext, mZAConfigOptions.mPersistentSecretKey, mZAConfigOptions.getEncryptors());
        }

        DbAdapter.getInstance(mContext, packageName, mZallDataEncrypt);
        mTrackTaskManager.setDataCollectEnable(mZAConfigOptions.isDataCollectEnable);

        if (mZAConfigOptions.mInvokeLog) {
            enableLog(mZAConfigOptions.mLogEnabled);
        } else {
            enableLog(configBundle.getBoolean("com.zalldata.zall.android.EnableLogging",
                    this.mDebugMode != ZallDataAPI.DebugMode.DEBUG_OFF));
        }
        ZALog.setDisableSDK(mZAConfigOptions.isDisableSDK);

        setServerUrl(serverURL);
        if (mZAConfigOptions.mEnableTrackAppCrash) {
            ZallDataExceptionHandler.enableAppCrash();
        }

        if (mZAConfigOptions.mFlushInterval == 0) {
            mZAConfigOptions.setFlushInterval(configBundle.getInt("com.zalldata.zall.android.FlushInterval",
                    15000));
        }

        if (mZAConfigOptions.mFlushBulkSize == 0) {
            mZAConfigOptions.setFlushBulkSize(configBundle.getInt("com.zalldata.zall.android.FlushBulkSize",
                    100));
        }

        if (mZAConfigOptions.mMaxCacheSize == 0) {
            mZAConfigOptions.setMaxCacheSize(32 * 1024 * 1024L);
        }

        if (mZAConfigOptions.isSubProcessFlushData && DbAdapter.getInstance().isFirstProcess()) {
            //如果是首个进程
            DbAdapter.getInstance().commitFirstProcessState(false);
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }

        this.mAutoTrack = configBundle.getBoolean("com.zalldata.zall.android.AutoTrack",
                false);
        if (mZAConfigOptions.mAutoTrackEventType != 0) {
            enableAutoTrack(mZAConfigOptions.mAutoTrackEventType);
            this.mAutoTrack = true;
        }

        if (!mZAConfigOptions.mInvokeHeatMapEnabled) {
            mZAConfigOptions.mHeatMapEnabled = configBundle.getBoolean("com.zalldata.zall.android.HeatMap",
                    false);
        }

        if (!mZAConfigOptions.mInvokeVisualizedEnabled) {
            mZAConfigOptions.mVisualizedEnabled = configBundle.getBoolean("com.zalldata.zall.android.VisualizedAutoTrack",
                    false);
        }

        enableTrackScreenOrientation(mZAConfigOptions.mTrackScreenOrientationEnabled);

        if (!TextUtils.isEmpty(mZAConfigOptions.mAnonymousId)) {
            identify(mZAConfigOptions.mAnonymousId);
        }

        if (mZAConfigOptions.isDisableSDK) {
            mEnableNetworkRequest = false;
            isChangeEnableNetworkFlag = true;
        }

        SHOW_DEBUG_INFO_VIEW = configBundle.getBoolean("com.zalldata.zall.android.ShowDebugInfoView",
                true);

        this.mDisableDefaultRemoteConfig = configBundle.getBoolean("com.zalldata.zall.android.DisableDefaultRemoteConfig",
                false);

        if (mZAConfigOptions.isDataCollectEnable) {
            mIsMainProcess = AppInfoUtils.isMainProcess(mContext, configBundle);
        }

        this.mDisableTrackDeviceId = configBundle.getBoolean("com.zalldata.zall.android.DisableTrackDeviceId",
                false);
    }

    protected void applyZAConfigOptions() {
        if (mZAConfigOptions.mEnableTrackAppCrash) {
            ZallDataExceptionHandler.enableAppCrash();
        }

        if (mZAConfigOptions.mAutoTrackEventType != 0) {
            this.mAutoTrack = true;
        }

        if (mZAConfigOptions.mInvokeLog) {
            enableLog(mZAConfigOptions.mLogEnabled);
        }

        enableTrackScreenOrientation(mZAConfigOptions.mTrackScreenOrientationEnabled);

        if (!TextUtils.isEmpty(mZAConfigOptions.mAnonymousId)) {
            identify(mZAConfigOptions.mAnonymousId);
        }

        //由于自定义属性依赖于可视化全埋点，所以只要开启自定义属性，默认打开可视化全埋点功能
        if (!mZAConfigOptions.mVisualizedEnabled && mZAConfigOptions.mVisualizedPropertiesEnabled) {
            ZALog.i(TAG, "当前已开启可视化全埋点自定义属性（enableVisualizedProperties），可视化全埋点采集开关已失效！");
            mZAConfigOptions.enableVisualizedAutoTrack(true);
        }
    }

    /**
     * 触发事件的暂停/恢复
     *
     * @param eventName 事件名称
     * @param isPause 设置是否暂停
     */
    protected void trackTimerState(final String eventName, final boolean isPause) {
        final long startTime = SystemClock.elapsedRealtime();
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    assertKey(eventName);
                    synchronized (mTrackTimer) {
                        EventTimer eventTimer = mTrackTimer.get(eventName);
                        if (eventTimer != null && eventTimer.isPaused() != isPause) {
                            eventTimer.setTimerState(isPause, startTime);
                        }
                    }
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        });
    }

    /**
     * 读取动态公共属性
     *
     * @return 动态公共属性
     */
    protected JSONObject getDynamicProperty() {
        JSONObject dynamicProperty = null;
        try {
            if (mDynamicSuperPropertiesCallBack != null) {
                dynamicProperty = mDynamicSuperPropertiesCallBack.getDynamicSuperProperties();
                assertPropertyTypes(dynamicProperty);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return dynamicProperty;
    }

    /**
     * 合并、去重静态公共属性与动态公共属性
     *
     * @param eventProperty 保存合并后属性的 JSON
     * @param dynamicProperty 动态公共属性
     */
    private void mergerDynamicAndSuperProperties(JSONObject eventProperty, JSONObject dynamicProperty) {
        JSONObject superProperties = getSuperProperties();
        if (dynamicProperty == null) {
            dynamicProperty = getDynamicProperty();
        }
        JSONObject removeDuplicateSuperProperties = ZallDataUtils.mergeSuperJSONObject(dynamicProperty, superProperties);
        ZallDataUtils.mergeJSONObject(removeDuplicateSuperProperties, eventProperty);
    }

    private void showDebugModeWarning() {
        try {
            if (mDebugMode == ZallDataAPI.DebugMode.DEBUG_OFF) {
                return;
            }
            if (TextUtils.isEmpty(mServerUrl)) {
                return;
            }
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String info = null;
                    if (mDebugMode == ZallDataAPI.DebugMode.DEBUG_ONLY) {
                        info = "现在您打开了 ZallData SDK 的 'DEBUG_ONLY' 模式，此模式下只校验数据但不导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    } else if (mDebugMode == ZallDataAPI.DebugMode.DEBUG_AND_TRACK) {
                        info = "现在您打开了卓尔 ZallData SDK 的 'DEBUG_AND_TRACK' 模式，此模式下校验数据并且导入数据，数据出错时会以 Toast 的方式提示开发者，请上线前一定使用 DEBUG_OFF 模式。";
                    }
                    CharSequence appName = AppInfoUtils.getAppName(mContext);
                    if (!TextUtils.isEmpty(appName)) {
                        info = String.format(Locale.CHINA, "%s：%s", appName, info);
                    }
                    Toast.makeText(mContext, info, Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            com.zalldata.zall.android.sdk.ZALog.printStackTrace(e);
        }
    }

    /**
     * @param eventName 事件名
     * @param eventProperties 事件属性
     * @return 该事件是否入库
     */
    private boolean isEnterDb(String eventName, JSONObject eventProperties) {
        boolean enterDb = true;
        if (mTrackEventCallBack != null) {
            ZALog.d(TAG, "SDK have set trackEvent callBack");
            try {
                enterDb = mTrackEventCallBack.onTrackEvent(eventName, eventProperties);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
            if (enterDb) {
                try {
                    Iterator<String> it = eventProperties.keys();
                    while (it.hasNext()) {
                        String key = it.next();
                        try {
                            assertKey(key);
                        } catch (Exception e) {
                            ZALog.printStackTrace(e);
                            return false;
                        }
                        Object value = eventProperties.opt(key);
                        if (!(value instanceof CharSequence || value instanceof Number || value
                                instanceof JSONArray || value instanceof Boolean || value instanceof Date)) {
                            ZALog.d(TAG, String.format("The property value must be an instance of " +
                                            "CharSequence/Number/Boolean/JSONArray/Date. [key='%s', value='%s', class='%s']",
                                    key,
                                    value == null ? "" : value.toString(),
                                    value == null ? "" : value.getClass().getCanonicalName()));
                            return false;
                        }

                        if ("app_crashed_reason".equals(key)) {
                            if (value instanceof String && ((String) value).length() > 8191 * 2) {
                                ZALog.d(TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']");
                                value = ((String) value).substring(0, 8191 * 2) + "$";
                            }
                        } else {
                            if (value instanceof String && ((String) value).length() > 8191) {
                                ZALog.d(TAG, "The property value is too long. [key='" + key
                                        + "', value='" + value.toString() + "']");
                                value = ((String) value).substring(0, 8191) + "$";
                            }
                        }
                        if (value instanceof Date) {
                            eventProperties.put(key, TimeUtils.formatDate((Date) value, Locale.CHINA));
                        } else {
                            eventProperties.put(key, value);
                        }
                    }
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        }
        return enterDb;
    }

    private void trackEventInternal(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties,
                                    String distinctId, String loginId, final String originalDistinctId, final EventTimer eventTimer) throws JSONException {
        String libDetail = null;
        String lib_version = VERSION;
        String appEnd_app_version = null;
        long eventTime = System.currentTimeMillis();
        JSONObject libProperties = new JSONObject();
        if (null != properties) {
            try {
                if (properties.has("$lib_detail")) {
                    libDetail = properties.getString("$lib_detail");
                    properties.remove("$lib_detail");
                }
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
            try {
                // 单独处理 $AppStart 和 $AppEnd 的时间戳
                if ("$AppEnd".equals(eventName)) {
                    long appEndTime = properties.optLong("event_time");
                    // 退出时间戳不合法不使用，2000 为打点间隔时间戳
                    if (appEndTime > 2000) {
                        eventTime = appEndTime;
                    }
                    String appEnd_lib_version = properties.optString("$lib_version");
                    appEnd_app_version = properties.optString("$app_version");
                    if (!TextUtils.isEmpty(appEnd_lib_version)) {
                        lib_version = appEnd_lib_version;
                    } else {
                        properties.remove("$lib_version");
                    }

                    if (TextUtils.isEmpty(appEnd_app_version)) {
                        properties.remove("$app_version");
                    }

                    properties.remove("event_time");
                } else if ("$AppStart".equals(eventName)) {
                    long appStartTime = properties.optLong("event_time");
                    if (appStartTime > 0) {
                        eventTime = appStartTime;
                    }
                    properties.remove("event_time");
                }
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
            ZallDataUtils.mergeJSONObject(properties, sendProperties);
            if (eventType.isTrack()) {
                if ("autoTrack".equals(properties.optString("$lib_method"))) {
                    libProperties.put("$lib_method", "autoTrack");
                } else {
                    libProperties.put("$lib_method", "code");
                    sendProperties.put("$lib_method", "code");
                }
            } else {
                libProperties.put("$lib_method", "code");
            }
        } else {
            libProperties.put("$lib_method", "code");
            if (eventType.isTrack()) {
                sendProperties.put("$lib_method", "code");
            }
        }

        if (null != eventTimer) {
            try {
                double duration = Double.parseDouble(eventTimer.duration());
                if (duration > 0) {
                    sendProperties.put("event_duration", duration);
                }
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }

        libProperties.put("$lib", "Android");
        libProperties.put("$lib_version", lib_version);
        if (TextUtils.isEmpty(appEnd_app_version)) {
            mZAContextManager.addKeyIfExist(libProperties, "$app_version");
        } else {
            libProperties.put("$app_version", appEnd_app_version);
        }

        //update lib $app_version from super properties
        JSONObject superProperties = mSuperProperties.get();
        if (superProperties != null) {
            if (superProperties.has("$app_version")) {
                libProperties.put("$app_version", superProperties.get("$app_version"));
            }
        }

        final JSONObject dataObj = new JSONObject();

        try {
            SecureRandom random = new SecureRandom();
            dataObj.put("_track_id", random.nextInt());
        } catch (Exception e) {
            // ignore
        }

        dataObj.put("time", eventTime);
        dataObj.put("type", eventType.getEventType());
        String anonymousId = getAnonymousId();
        try {
            if (sendProperties.has("$project")) {
                dataObj.put("project", sendProperties.optString("$project"));
                sendProperties.remove("$project");
            }

            if (sendProperties.has("$token")) {
                dataObj.put("token", sendProperties.optString("$token"));
                sendProperties.remove("$token");
            }

            if (sendProperties.has("$time")) {
                try {
                    Object timeDate = sendProperties.opt("$time");
                    if (timeDate instanceof Date) {
                        if (TimeUtils.isDateValid((Date) timeDate)) {
                            dataObj.put("time", ((Date) timeDate).getTime());
                        }
                    }
                } catch (Exception ex) {
                    ZALog.printStackTrace(ex);
                }
                sendProperties.remove("$time");
            }

            //针对 SF 弹窗展示事件特殊处理
            if ("$PlanPopupDisplay".equals(eventName)) {
                if (sendProperties.has("$sf_internal_anonymous_id")) {
                    anonymousId = sendProperties.optString("$sf_internal_anonymous_id");
                    sendProperties.remove("$sf_internal_anonymous_id");
                }

                if (sendProperties.has("$sf_internal_login_id")) {
                    loginId = sendProperties.optString("$sf_internal_login_id");
                    sendProperties.remove("$sf_internal_login_id");
                }
                if (!TextUtils.isEmpty(loginId)) {
                    distinctId = loginId;
                } else {
                    distinctId = anonymousId;
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }

        if (TextUtils.isEmpty(distinctId)) {// 如果为空，则说明没有 loginId，所以重新设置当时状态的匿名 Id
            dataObj.put("distinct_id", getAnonymousId());
        } else {
            dataObj.put("distinct_id", distinctId);
        }

        if (!TextUtils.isEmpty(loginId)) {
            dataObj.put("login_id", loginId);
        }
        dataObj.put("anonymous_id", anonymousId);

        dataObj.put("lib", libProperties);

        if (eventType == EventType.TRACK) {
            dataObj.put("event", eventName);
            //是否首日访问
            sendProperties.put("$is_first_day", isFirstDay(eventTime));
        } else if (eventType == EventType.TRACK_SIGNUP) {
            dataObj.put("event", eventName);
            dataObj.put("original_id", originalDistinctId);
        }

        if (mAutoTrack && properties != null) {
            if (ZallDataAPI.AutoTrackEventType.isAutoTrackType(eventName)) {
                ZallDataAPI.AutoTrackEventType trackEventType = ZallDataAPI.AutoTrackEventType.autoTrackEventTypeFromEventName(eventName);
                if (trackEventType != null) {
                    if (!isAutoTrackEventTypeIgnored(trackEventType)) {
                        if (properties.has("$screen_name")) {
                            String screenName = properties.getString("$screen_name");
                            if (!TextUtils.isEmpty(screenName)) {
                                String[] screenNameArray = screenName.split("\\|");
                                if (screenNameArray.length > 0) {
                                    libDetail = String.format("%s##%s##%s##%s", screenNameArray[0], "", "", "");
                                }
                            }
                        }
                    }
                }
            }
        }

        if (TextUtils.isEmpty(libDetail)) {
            StackTraceElement[] trace = (new Exception()).getStackTrace();
            if (trace.length > 1) {
                StackTraceElement traceElement = trace[0];
                libDetail = String.format("%s##%s##%s##%s", traceElement
                                .getClassName(), traceElement.getMethodName(), traceElement.getFileName(),
                        traceElement.getLineNumber());
            }
        }

        libProperties.put("$lib_detail", libDetail);

        //防止用户自定义事件以及公共属性可能会加 $device_id 属性，导致覆盖 sdk 原始的 $device_id 属性值
        if (sendProperties.has("$device_id")) {//由于 profileSet 等类型事件没有 $device_id 属性，故加此判断
            mZAContextManager.addKeyIfExist(sendProperties, "$device_id");
        }
        if (eventType.isTrack()) {
            boolean isEnterDb = isEnterDb(eventName, sendProperties);
            if (!isEnterDb) {
                ZALog.d(TAG, eventName + " event can not enter database");
                return;
            }
            if (!isTrackEventWithPluginVersion && !sendProperties.has("$lib_plugin_version")) {
                JSONArray libPluginVersion = getPluginVersion();
                if (libPluginVersion == null) {
                    isTrackEventWithPluginVersion = true;
                } else {
                    try {
                        sendProperties.put("$lib_plugin_version", libPluginVersion);
                        isTrackEventWithPluginVersion = true;
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                }
            }
        }
        dataObj.put("properties", sendProperties);

        try {
            if (mEventListenerList != null && eventType.isTrack()) {
                for (ZAEventListener eventListener : mEventListenerList) {
                    eventListener.trackEvent(dataObj);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }

        try {
            if (mFunctionListenerList != null && eventType.isTrack()) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("eventJSON", dataObj);
                for (ZAFunctionListener listener : mFunctionListenerList) {
                    listener.call("trackEvent", jsonObject);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }

        mMessages.enqueueEventMessage(eventType.getEventType(), dataObj);
        if ("$AppStart".equals(eventName)) {
            mZAContextManager.setAppStartSuccess(true);
        }
        if (ZALog.isLogEnabled()) {
            ZALog.i(TAG, "track event:\n" + JSONUtils.formatJson(dataObj.toString()));
        }
    }

    /**
     * 如果没有授权时，需要将已执行的的缓存队列切换到真正的 TaskQueue 中
     */
    private void transformEventTaskQueue(final EventType eventType, final String eventName, final JSONObject properties, final JSONObject sendProperties,
                                         final String distinctId, final String loginId, final String originalDistinctId, final EventTimer eventTimer) {
        try {
            if (!sendProperties.has("$time") && !("$AppStart".equals(eventName) || "$AppEnd".equals(eventName))) {
                sendProperties.put("$time", new Date(System.currentTimeMillis()));
            }
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
        mTrackTaskManager.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    if (eventType.isTrack()) {
                        JSONObject jsonObject = new JSONObject(mZAContextManager.getDeviceInfo());
                        JSONUtils.mergeDistinctProperty(jsonObject, sendProperties);
                    }
                    if ("$SignUp".equals(eventName)) {// 如果是 "$SignUp" 则需要重新补上 originalId
                        trackEventInternal(eventType, eventName, properties, sendProperties, distinctId, loginId, getAnonymousId(), eventTimer);
                    } else {
                        trackEventInternal(eventType, eventName, properties, sendProperties, distinctId, loginId, originalDistinctId, eventTimer);
                    }
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        });
    }

    private void transformH5TaskQueue(String eventInfo) {
        try {
            final JSONObject eventObject = new JSONObject(eventInfo);
            JSONObject propertiesObject = eventObject.optJSONObject("properties");
            if (propertiesObject != null && !propertiesObject.has("$time")) {
                propertiesObject.put("$time", System.currentTimeMillis());
            }
            if (ZALog.isLogEnabled()) {
                ZALog.i(TAG, "track H5, isDataCollectEnable = false, eventInfo = " + JSONUtils.formatJson(eventInfo));
            }
            mTrackTaskManager.transformTaskQueue(new Runnable() {
                @Override
                public void run() {
                    try {
                        trackEventH5(eventObject.toString());
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                }
            });
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    private void transformItemTaskQueue(final String itemType, final String itemId, final String eventType, final long time, final JSONObject properties) {
        if (ZALog.isLogEnabled()) {
            ZALog.i(TAG, "track item, isDataCollectEnable = false, itemType = " + itemType + ",itemId = " + itemId);
        }
        mTrackTaskManager.transformTaskQueue(new Runnable() {
            @Override
            public void run() {
                try {
                    trackItemEvent(itemType, itemId, eventType, time, properties);
                } catch (Exception e) {
                    ZALog.printStackTrace(e);
                }
            }
        });
    }

    private JSONArray getPluginVersion() {
        try {
            if (!TextUtils.isEmpty(ZallDataAPI.ANDROID_PLUGIN_VERSION)) {
                ZALog.i(TAG, "android plugin version: " + ZallDataAPI.ANDROID_PLUGIN_VERSION);
                JSONArray libPluginVersion = new JSONArray();
                libPluginVersion.put("android:" + ZallDataAPI.ANDROID_PLUGIN_VERSION);
                return libPluginVersion;
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return null;
    }

    /**
     * 注册 ActivityLifecycleCallbacks
     */
    private void registerLifecycleCallbacks() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                final Application app = (Application) mContext.getApplicationContext();
                final ZallDataActivityLifecycleCallbacks lifecycleCallbacks = new ZallDataActivityLifecycleCallbacks();
                app.registerActivityLifecycleCallbacks(lifecycleCallbacks);
                app.registerActivityLifecycleCallbacks(AppStateManager.getInstance());
                mActivityLifecycleCallbacks = new ActivityLifecycleCallbacks((ZallDataAPI) this, mFirstStart, mFirstDay, mContext);
                lifecycleCallbacks.addActivityLifecycleCallbacks(mActivityLifecycleCallbacks);
                ZallDataExceptionHandler.addExceptionListener(mActivityLifecycleCallbacks);
                FragmentTrackHelper.addFragmentCallbacks(new FragmentViewScreenCallbacks());

                if (mZAConfigOptions.isTrackPageLeave()) {
                    ActivityPageLeaveCallbacks pageLeaveCallbacks = new ActivityPageLeaveCallbacks();
                    lifecycleCallbacks.addActivityLifecycleCallbacks(pageLeaveCallbacks);
                    ZallDataExceptionHandler.addExceptionListener(pageLeaveCallbacks);

                    FragmentPageLeaveCallbacks fragmentPageLeaveCallbacks = new FragmentPageLeaveCallbacks();
                    FragmentTrackHelper.addFragmentCallbacks(fragmentPageLeaveCallbacks);
                    ZallDataExceptionHandler.addExceptionListener(fragmentPageLeaveCallbacks);
                }
                if (mZAConfigOptions.isEnableTrackPush()) {
                    lifecycleCallbacks.addActivityLifecycleCallbacks(new PushLifecycleCallbacks());
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 注册 ContentObserver 监听
     */
    private void registerObserver() {
        // 注册跨进程业务的 ContentObserver 监听
        ZallDataContentObserver contentObserver = new ZallDataContentObserver();
        ContentResolver contentResolver = mContext.getContentResolver();
        contentResolver.registerContentObserver(DbParams.getInstance().getDataCollectUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getSessionTimeUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getLoginIdUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getDisableSDKUri(), false, contentObserver);
        contentResolver.registerContentObserver(DbParams.getInstance().getEnableSDKUri(), false, contentObserver);
    }

    /**
     * $AppDeeplinkLaunch 事件是否包含 $ios_install_source 属性
     *
     * @return boolean
     */
    public boolean isDeepLinkInstallSource() {
        return mEnableDeepLinkInstallSource;
    }

    /**
     * 延迟初始化任务
     */
    protected void delayInitTask() {
        mTrackTaskManager.addTrackEventTask(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isSaveDeepLinkInfo()) {
                        ChannelUtils.loadUtmByLocal(mContext);
                    } else {
                        ChannelUtils.clearLocalUtm(mContext);
                    }
                    registerNetworkListener();
                } catch (Exception ex) {
                    ZALog.printStackTrace(ex);
                }
            }
        });
    }

    /**
     * 重新读取运营商信息
     *
     * @param property Property
     */
    private void getCarrier(JSONObject property) {
        try {
            if (TextUtils.isEmpty(property.optString("$carrier")) && mZAConfigOptions.isDataCollectEnable) {
                String carrier = ZallDataUtils.getCarrier(mContext);
                if (!TextUtils.isEmpty(carrier)) {
                    property.put("$carrier", carrier);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }
}
