/*
 * Created by guo on 2021/07/29.
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

package com.zalldata.analytics.android.sdk.autotrack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ScreenAutoTracker;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.ZallDataActivityLifecycleCallbacks;
import com.zalldata.analytics.android.sdk.ZallDataExceptionHandler;
import com.zalldata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.zalldata.analytics.android.sdk.data.adapter.DbAdapter;
import com.zalldata.analytics.android.sdk.data.persistent.PersistentFirstDay;
import com.zalldata.analytics.android.sdk.data.persistent.PersistentFirstStart;
import com.zalldata.analytics.android.sdk.deeplink.DeepLinkManager;
import com.zalldata.analytics.android.sdk.dialog.ZallDataDialogUtils;
import com.zalldata.analytics.android.sdk.util.AopUtil;
import com.zalldata.analytics.android.sdk.util.AppInfoUtils;
import com.zalldata.analytics.android.sdk.util.ZADataHelper;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;
import com.zalldata.analytics.android.sdk.util.TimeUtils;
import com.zalldata.analytics.android.sdk.visual.HeatMapService;
import com.zalldata.analytics.android.sdk.visual.VisualizedAutoTrackService;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class ActivityLifecycleCallbacks implements ZallDataActivityLifecycleCallbacks.ZAActivityLifecycleCallbacks, ZallDataExceptionHandler.ZAExceptionListener {
    private static final String TAG = "ZA.ActivityLifecycleCallbacks";
    private static final String EVENT_TIME = "event_time";
    private static final String EVENT_DURATION = "event_duration";
    private static final String LIB_VERSION = "$lib_version";
    private static final String APP_VERSION = "$app_version";
    private final ZallDataAPI mZallDataInstance;
    private final Context mContext;
    private final PersistentFirstStart mFirstStart;
    private final PersistentFirstDay mFirstDay;
    private boolean resumeFromBackground = false;
    private final DbAdapter mDbAdapter;
    private JSONObject activityProperty = new JSONObject();
    private final JSONObject endDataProperty = new JSONObject();
    private JSONObject mDeepLinkProperty = new JSONObject();
    private int mStartActivityCount;
    private int mStartTimerCount;
    private long mStartTime;
    // $AppStart 事件的时间戳
    private final String APP_START_TIME = "app_start_time";
    // $AppEnd 事件属性
    private final String APP_END_DATA = "app_end_data";
    // App 是否重置标记位
    private final String APP_RESET_STATE = "app_reset_state";
    private final String TIME = "time";
    private final String ELAPSE_TIME = "elapse_time";
    private Handler mHandler;
    /* 兼容由于在魅族手机上退到后台后，线程会被休眠，导致 $AppEnd 无法触发，造成再次打开重复发送。*/
    private long messageReceiveTime = 0L;
    private final int MESSAGE_CODE_APP_END = 0;
    private final int MESSAGE_CODE_START = 100;
    private final int MESSAGE_CODE_STOP = 200;
    private final int MESSAGE_CODE_TIMER = 300;
    /**
     * 打点时间间隔：2000 毫秒
     */
    private static final int TIME_INTERVAL = 2000;
    private boolean mDataCollectState;

    private Set<Integer> hashSet = new HashSet<>();

    public ActivityLifecycleCallbacks(ZallDataAPI instance, PersistentFirstStart firstStart,
                                          PersistentFirstDay firstDay, Context context) {
        this.mZallDataInstance = instance;
        this.mFirstStart = firstStart;
        this.mFirstDay = firstDay;
        this.mDbAdapter = DbAdapter.getInstance();
        this.mContext = context;
        mDataCollectState = ZallDataAPI.getConfigOptions().isDataCollectEnable();
        initHandler();
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {
        if (!ZallDataDialogUtils.isSchemeActivity(activity)) {
            ZallDataUtils.handleSchemeUrl(activity, activity.getIntent());
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (!ZallDataDialogUtils.isSchemeActivity(activity) && !hasActivity(activity)) {
            if (mStartActivityCount == 0) {
                // 第一个页面进行页面信息解析
                buildScreenProperties(activity);
            }
            sendActivityHandleMessage(MESSAGE_CODE_START);
            addActivity(activity);
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        try {
            buildScreenProperties(activity);
            if (mZallDataInstance.isAutoTrackEnabled() && !mZallDataInstance.isActivityAutoTrackAppViewScreenIgnored(activity.getClass())
                    && !mZallDataInstance.isAutoTrackEventTypeIgnored(ZallDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
                JSONObject properties = new JSONObject();
                ZallDataUtils.mergeJSONObject(activityProperty, properties);
                if (activity instanceof ScreenAutoTracker) {
                    ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) activity;
                    JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                    if (otherProperties != null) {
                        ZallDataUtils.mergeJSONObject(otherProperties, properties);
                    }
                }
                // 合并渠道信息到 $AppViewScreen 事件中
                DeepLinkManager.mergeDeepLinkProperty(properties);
                DeepLinkManager.resetDeepLinkProcessor();
                JSONObject eventProperties = ZADataHelper.appendLibMethodAutoTrack(properties);
                mZallDataInstance.trackViewScreen(ZallDataUtils.getScreenUrl(activity), eventProperties);
            }
        } catch (Throwable e) {
            ZALog.i(TAG, e);
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (!ZallDataDialogUtils.isSchemeActivity(activity) && hasActivity(activity)) {
            sendActivityHandleMessage(MESSAGE_CODE_STOP);
            removeActivity(activity);
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {

    }

    @Override
    public void onNewIntent(Intent intent) {

    }


    private void initHandler() {
        try {
            HandlerThread handlerThread = new HandlerThread("ZALL_DATA_THREAD");
            handlerThread.start();
            mHandler = new Handler(handlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    int code = msg.what;
                    switch (code) {
                        case MESSAGE_CODE_START:
                            handleStartedMessage(msg);
                            break;
                        case MESSAGE_CODE_STOP:
                            handleStoppedMessage(msg);
                            break;
                        case MESSAGE_CODE_TIMER:
                            if (mZallDataInstance.isAutoTrackEnabled() && isAutoTrackAppEnd()) {
                                generateAppEndData(System.currentTimeMillis(), SystemClock.elapsedRealtime());
                            } else if (!mZallDataInstance.isAutoTrackEnabled() && mStartTime > 0) {//调用 disableSDK 接口时重新更新 duration
                                mStartTime = 0;
                                DbAdapter.getInstance().commitAppStartTime(mStartTime);
                                generateAppEndData(System.currentTimeMillis(), SystemClock.elapsedRealtime());
                            }

                            if (mStartTimerCount > 0) {
                                mHandler.sendEmptyMessageDelayed(MESSAGE_CODE_TIMER, TIME_INTERVAL);
                            }
                            break;
                        case MESSAGE_CODE_APP_END:
                            if (messageReceiveTime != 0 && SystemClock.elapsedRealtime() - messageReceiveTime < mZallDataInstance.getSessionIntervalTime()) {
                                ZALog.i(TAG, "$AppEnd 事件已触发。");
                                return;
                            }
                            messageReceiveTime = SystemClock.elapsedRealtime();
                            Bundle bundle = msg.getData();
                            String endData = bundle.getString(APP_END_DATA);
                            boolean resetState = bundle.getBoolean(APP_RESET_STATE);
                            // 如果是正常的退到后台，需要重置标记位
                            if (resetState) {
                                resetState();
                                // 对于 Unity 多进程跳转的场景，需要在判断一下
                                if (DbAdapter.getInstance().getActivityCount() <= 0) {
                                    trackAppEnd(endData);
                                }
                            } else {
                                trackAppEnd(endData);
                            }
                            break;
                    }
                }
            };
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    private void handleStartedMessage(Message message) {
        boolean isSessionTimeout;
        try {
            mStartActivityCount = mDbAdapter.getActivityCount();
            mDbAdapter.commitActivityCount(++mStartActivityCount);
            // 如果是第一个页面
            if (mStartActivityCount == 1) {
                if (mZallDataInstance.getConfigOptions().isSaveDeepLinkInfo()) {// 保存 utm 信息时,在 endData 中合并保存的 latestUtm 信息。
                    ZallDataUtils.mergeJSONObject(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                }
                mHandler.removeMessages(MESSAGE_CODE_APP_END);
                isSessionTimeout = isSessionTimeOut();
                if (isSessionTimeout) {
                    // 超时尝试补发 $AppEnd
                    mHandler.sendMessage(obtainAppEndMessage(false));
                    checkFirstDay();
                    // XXX: 注意内部执行顺序
                    boolean firstStart = mFirstStart.get();
                    try {
                        mZallDataInstance.appBecomeActive();

                        //从后台恢复，从缓存中读取 SDK 控制配置信息
                        if (resumeFromBackground) {
                            //先从缓存中读取 SDKConfig
                            mZallDataInstance.getRemoteManager().applySDKConfigFromCache();
                            mZallDataInstance.resumeTrackScreenOrientation();
//                    mZallDataInstance.resumeTrackTaskThread();
                        }
                        //每次启动 App，重新拉取最新的配置信息
                        mZallDataInstance.getRemoteManager().pullSDKConfigFromServer();
                    } catch (Exception ex) {
                        ZALog.printStackTrace(ex);
                    }
                    Bundle bundle = message.getData();
                    try {
                        if (mZallDataInstance.isAutoTrackEnabled() && !mZallDataInstance.isAutoTrackEventTypeIgnored(ZallDataAPI.AutoTrackEventType.APP_START)) {
                            if (firstStart) {
                                mFirstStart.commit(false);
                            }
                            JSONObject properties = new JSONObject();
                            properties.put("$resume_from_background", resumeFromBackground);
                            properties.put("$is_first_time", firstStart);
                            ZallDataUtils.mergeJSONObject(activityProperty, properties);
                            // 合并渠道信息到 $AppStart 事件中
                            if (mDeepLinkProperty != null) {
                                ZallDataUtils.mergeJSONObject(mDeepLinkProperty, properties);
                                mDeepLinkProperty = null;
                            }
                            // 读取 Message 中的时间戳
                            long eventTime = bundle.getLong(TIME);
                            properties.put("event_time", eventTime > 0 ? eventTime : System.currentTimeMillis());
                            mZallDataInstance.trackAutoEvent("$AppStart", properties);
                            ZallDataAPI.sharedInstance().flush();
                        }
                    } catch (Exception e) {
                        ZALog.i(TAG, e);
                    }

                    updateStartTime(bundle.getLong(ELAPSE_TIME));

                    if (resumeFromBackground) {
                        try {
                            HeatMapService.getInstance().resume();
                            VisualizedAutoTrackService.getInstance().resume();
                        } catch (Exception e) {
                            ZALog.printStackTrace(e);
                        }
                    }

                    // 下次启动时，从后台恢复
                    resumeFromBackground = true;
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
            updateStartTime(SystemClock.elapsedRealtime());
        }

        try {
            if (mStartTimerCount++ == 0) {
                /*
                 * 在启动的时候开启打点，退出时停止打点，在此处可以防止两点：
                 *  1. App 在 onResume 之前 Crash，导致只有启动没有退出；
                 *  2. 多进程的情况下只会开启一个打点器；
                 */
                mHandler.sendEmptyMessage(MESSAGE_CODE_TIMER);
            }
        } catch (Exception exception) {
            ZALog.printStackTrace(exception);
        }
    }

    private void handleStoppedMessage(Message message) {
        try {
            // 停止计时器，针对跨进程的情况，要停止当前进程的打点器
            mStartTimerCount--;
            if (mStartTimerCount <= 0) {
                mHandler.removeMessages(MESSAGE_CODE_TIMER);
                mStartTimerCount = 0;
                mStartTime = 0;
            }

            mStartActivityCount = mDbAdapter.getActivityCount();
            mStartActivityCount = mStartActivityCount > 0 ? --mStartActivityCount : 0;
            mDbAdapter.commitActivityCount(mStartActivityCount);

            /*
             * 为了处理跨进程之间跳转 Crash 的情况，由于在 ExceptionHandler 中进行重置，
             * 所以会引起的计数器小于 0 的情况。
             */
            if (mStartActivityCount <= 0) {
                // 主动 flush 数据
                mZallDataInstance.flush();
                Bundle bundle = message.getData();
                generateAppEndData(bundle.getLong(TIME), bundle.getLong(ELAPSE_TIME));
                mHandler.sendMessageDelayed(obtainAppEndMessage(true), mZallDataInstance.getSessionIntervalTime());
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 发送 $AppEnd 事件
     *
     * @param jsonEndData $AppEnd 事件属性
     */
    private void trackAppEnd(String jsonEndData) {
        try {
            if (mZallDataInstance.isAutoTrackEnabled() && isAutoTrackAppEnd() && !TextUtils.isEmpty(jsonEndData)) {
                JSONObject property = new JSONObject(jsonEndData);
                if (property.has("track_timer")) {
                    property.put(EVENT_TIME, property.optLong("track_timer") + TIME_INTERVAL);
                    property.remove("event_timer");     // 删除老版本冗余属性
                    property.remove("track_timer");     // 删除老版本冗余属性
                }
                property.remove(APP_START_TIME);
                mZallDataInstance.trackAutoEvent("$AppEnd", property);
                mDbAdapter.commitAppEndData(""); // 保存的信息只使用一次就置空，防止后面状态错乱再次发送。
                mZallDataInstance.flush();
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 存储当前的 AppEnd 事件关键信息
     */
    private void generateAppEndData(long eventTime, long endElapsedTime) {
        try {
            // 如果初始未非合规状态，则需要判断同意合规后才打点
            if (!mDataCollectState && !mZallDataInstance.getZAContextManager().isAppStartSuccess()) {
                return;
            }
            mDataCollectState = true;
            // 同意合规时进行打点记录
            if (ZallDataAPI.getConfigOptions().isDataCollectEnable()) {
                if (mStartTime == 0) {//多进程切换要重新读取
                    mStartTime = DbAdapter.getInstance().getAppStartTime();
                }
                if (mStartTime != 0) {
                    endDataProperty.put(EVENT_DURATION, TimeUtils.duration(mStartTime, endElapsedTime));
                } else {
                    endDataProperty.remove(EVENT_DURATION);
                }
                endDataProperty.put(APP_START_TIME, mStartTime);
                endDataProperty.put(EVENT_TIME, eventTime + TIME_INTERVAL);
                endDataProperty.put(APP_VERSION, AppInfoUtils.getAppVersionName(mContext));
                endDataProperty.put(LIB_VERSION, ZallDataAPI.sharedInstance().getSDKVersion());
                // 合并 $utm 信息
                ChannelUtils.mergeUtmToEndData(ChannelUtils.getLatestUtmProperties(), endDataProperty);
                mDbAdapter.commitAppEndData(endDataProperty.toString());
            }
        } catch (Throwable e) {
            ZALog.d(TAG, e.getMessage());
        }
    }

    /**
     * 判断是否超出 Session 时间间隔
     *
     * @return true 超时，false 未超时
     */
    private boolean isSessionTimeOut() {
        long currentTime = Math.max(System.currentTimeMillis(), 946656000000L);
        long endTrackTime = 0;
        try {
            String endData = DbAdapter.getInstance().getAppEndData();
            if (!TextUtils.isEmpty(endData)) {
                JSONObject endDataJsonObject = new JSONObject(endData);
                endTrackTime = endDataJsonObject.optLong(EVENT_TIME) - TIME_INTERVAL; // 获取 $AppEnd 打点时间戳
                if (mStartTime == 0) {// 如果二次打开，此时从本地更新启动时间戳
                    updateStartTime(endDataJsonObject.optLong(APP_START_TIME));
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return Math.abs(currentTime - endTrackTime) > mZallDataInstance.getSessionIntervalTime();
    }

    /**
     * 更新启动时间戳
     * @param startElapsedTime 启动时间戳
     */
    private void updateStartTime(long startElapsedTime) {
        try {
            // 设置启动时间戳
            mStartTime = startElapsedTime;
            mDbAdapter.commitAppStartTime(startElapsedTime > 0 ? startElapsedTime : SystemClock.elapsedRealtime());   // 防止动态开启 $AppEnd 时，启动时间戳不对的问题。
        } catch (Exception ex) {
            try {
                // 出现异常，在重新存储一次，防止使用原有的时间戳造成时长计算错误
                mDbAdapter.commitAppStartTime(startElapsedTime > 0 ? startElapsedTime : SystemClock.elapsedRealtime());
            } catch (Exception exception) {
                // ignore
            }
        }
    }

    /**
     * 发送处理 Activity 生命周期的 Message
     *
     * @param type 消息类型
     */
    private void sendActivityHandleMessage(int type) {
        Message message = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putLong(TIME, System.currentTimeMillis());
        bundle.putLong(ELAPSE_TIME, SystemClock.elapsedRealtime());
        message.what = type;
        message.setData(bundle);
        mHandler.sendMessage(message);
    }

    /**
     * 构建 Message 对象
     *
     * @param resetState 是否重置状态
     * @return Message
     */
    private Message obtainAppEndMessage(boolean resetState) {
        Message message = Message.obtain(mHandler);
        message.what = MESSAGE_CODE_APP_END;
        Bundle bundle = new Bundle();
        bundle.putString(APP_END_DATA, DbAdapter.getInstance().getAppEndData());
        bundle.putBoolean(APP_RESET_STATE, resetState);
        message.setData(bundle);
        return message;
    }

    /**
     * AppEnd 正常结束时，重置一些设置状态
     */
    private void resetState() {
        try {
            mZallDataInstance.stopTrackScreenOrientation();
            mZallDataInstance.getRemoteManager().resetPullSDKConfigTimer();
            HeatMapService.getInstance().stop();
            VisualizedAutoTrackService.getInstance().stop();
            mZallDataInstance.appEnterBackground();
            resumeFromBackground = true;
            mZallDataInstance.clearLastScreenUrl();
//            mZallDataInstance.stopTrackTaskThread();
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 检查更新首日逻辑
     */
    private void checkFirstDay() {
        if (mFirstDay.get() == null && ZallDataAPI.getConfigOptions().isDataCollectEnable()) {
            mFirstDay.commit(TimeUtils.formatTime(System.currentTimeMillis(), TimeUtils.YYYY_MM_DD));
        }
    }

    private boolean isAutoTrackAppEnd() {
        return !mZallDataInstance.isAutoTrackEventTypeIgnored(ZallDataAPI.AutoTrackEventType.APP_END);
    }

    private void buildScreenProperties(Activity activity) {
        activityProperty = AopUtil.buildTitleNoAutoTrackerProperties(activity);
        ZallDataUtils.mergeJSONObject(activityProperty, endDataProperty);
        if (!ZallDataAPI.getConfigOptions().isDisableSDK() && isDeepLinkParseSuccess(activity)) {
            // 清除 AppEnd 中的 DeepLink 信息
            ChannelUtils.removeDeepLinkInfo(endDataProperty);
            // 合并渠道信息到 $AppStart 事件中
            if (mDeepLinkProperty == null) {
                mDeepLinkProperty = new JSONObject();
            }
            DeepLinkManager.mergeDeepLinkProperty(mDeepLinkProperty);
        }
    }

    /**
     * DeepLink 信息是否解析成功
     *
     * @param activity Activity 页面
     * @return DeepLink 是否成功解析
     */
    private boolean isDeepLinkParseSuccess(Activity activity) {
        try {
            if(!ZallDataUtils.isUniApp() || !ChannelUtils.isDeepLinkBlackList(activity)) {
                Intent intent = activity.getIntent();
                if (intent != null && intent.getData() != null) {
                    //判断 deepLink 信息是否已处理过
                    if (!intent.getBooleanExtra(DeepLinkManager.IS_ANALYTICS_DEEPLINK, false)) {
                        if (DeepLinkManager.parseDeepLink(activity, mZallDataInstance.getConfigOptions().isSaveDeepLinkInfo(), mZallDataInstance.getDeepLinkCallback())) {
                            intent.putExtra(DeepLinkManager.IS_ANALYTICS_DEEPLINK, true);
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            ZALog.i(TAG, ex.getMessage());
        }
        return false;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        /*
         * 异常的情况会出现两种：
         * 1. 未完成 $AppEnd 事件，触发的异常，此时需要记录下 AppEndTime
         * 2. 完成了 $AppEnd 事件，下次启动时触发的异常。还未及时更新 $AppStart 的时间戳，导致计算时长偏大，所以需要重新更新启动时间戳
         */
        if (TextUtils.isEmpty(DbAdapter.getInstance().getAppEndData())) {
            DbAdapter.getInstance().commitAppStartTime(SystemClock.elapsedRealtime());
        }

        if (ZallDataAPI.getConfigOptions().isMultiProcessFlush()) {
            DbAdapter.getInstance().commitSubProcessFlushState(false);
        }

        // 注意这里要重置为 0，对于跨进程的情况，如果子进程崩溃，主进程但是没崩溃，造成统计个数异常，所以要重置为 0。
        DbAdapter.getInstance().commitActivityCount(0);
    }

    void addActivity(Activity activity) {
        if (activity != null) {
            hashSet.add(activity.hashCode());
        }
    }

    boolean hasActivity(Activity activity) {
        if (activity != null) {
            return hashSet.contains(activity.hashCode());
        }
        return false;
    }

    void removeActivity(Activity activity) {
        if (activity != null) {
            hashSet.remove(activity.hashCode());
        }
    }
}
