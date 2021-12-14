/*
 * Created by guo on 2020/1/17.
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

package com.zalldata.zall.android.sdk.aop.push;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;

import com.zalldata.zall.android.sdk.ZALog;
import com.zalldata.zall.android.sdk.ZallDataAPI;
import com.zalldata.zall.android.sdk.ThreadNameConstants;
import com.zalldata.zall.android.sdk.util.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PushProcess {
    private static final String ZA_PUSH_ID = "ZA_PUSH_ID";
    private static final String DIR_NAME = "sensors.push";
    private static final String TAG = "ZA.NotificationProcessor";
    private final static int GT_PUSH_MSG = 1;
    private static PushProcess INSTANCE;
    private final int myPid;
    private final AtomicInteger mZAIntentId;
    private final Map<String, NotificationInfo> mGeTuiPushInfoMap;
    private final Handler mPushHandler;
    private final boolean customizeEnable;
    private final WeakHashMap<PendingIntent, String> mPendingIntent2Ids;
    private WeakReference<Intent> mLastIntentRef;
    private File mPushFile;

    private PushProcess() {
        Context context = ZallDataAPI.sharedInstance().getContext();
        if (context != null) {
            this.mPushFile = new File(context.getFilesDir(), DIR_NAME);
        }
        mZAIntentId = new AtomicInteger();
        myPid = Process.myPid();
        customizeEnable = Build.VERSION.SDK_INT >= 19;
        mPendingIntent2Ids = new WeakHashMap<>();
        mGeTuiPushInfoMap = new HashMap<>();
        HandlerThread thread = new HandlerThread(ThreadNameConstants.THREAD_PUSH_HANDLER);
        thread.start();
        mPushHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                int code = msg.what;
                if (code == GT_PUSH_MSG) {
                    try {
                        String msgId = (String) msg.obj;
                        if (!TextUtils.isEmpty(msgId) && mGeTuiPushInfoMap.containsKey(msgId)) {
                            NotificationInfo push = mGeTuiPushInfoMap.get(msgId);
                            mGeTuiPushInfoMap.remove(msgId);
                            if (push != null) {
                                PushAutoTrackHelper.trackGeTuiNotificationClicked(push.title, push.content, null, push.time);
                            }
                        }
                    } catch (Exception e) {
                        ZALog.printStackTrace(e);
                    }
                }
            }
        };
    }

    public static synchronized PushProcess getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PushProcess();
        }
        return INSTANCE;
    }

    public void hookIntent(Intent intent) {
        if (this.customizeEnable) {
            try {
                if (!isHooked(intent)) {
                    intent.putExtra(ZA_PUSH_ID, this.myPid + "-" + this.mZAIntentId.getAndIncrement());
                }
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    public void hookPendingIntent(Intent intent, PendingIntent pendingIntent) {
        if (this.customizeEnable) {
            String pushId = intent.getStringExtra(ZA_PUSH_ID);
            this.mPendingIntent2Ids.put(pendingIntent, pushId);
        }
    }

    public void onNotificationClick(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        try {
            WeakReference<Intent> weakReference = mLastIntentRef;
            if (weakReference == null || weakReference.get() != intent) {
                mLastIntentRef = new WeakReference<>(intent);
                if (customizeEnable) {
                    trackCustomizeClick(intent);
                }
                //只有 Activity 打开时，才尝试出发极光的推送
                if (context instanceof Activity) {
                    PushAutoTrackHelper.trackJPushOpenActivity(intent);
                }
                ZALog.i(TAG, "onNotificationClick");
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    public void onNotify(String tag, int id, final Notification notification) {
        if (this.customizeEnable) {
            try {
                if (notification.contentIntent != null) {
                    ZALog.i(TAG, "onNotify, tag: " + tag + ", id=" + id);
                    final NotificationInfo push = getNotificationInfo(notification);
                    if (push != null) {
                        mPushHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                checkAndStoreNotificationInfo(notification.contentIntent, push);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    void trackGTClickDelayed(String messageId, String title, String content) {
        try {
            Message message = Message.obtain();
            message.what = GT_PUSH_MSG;
            message.obj = messageId;
            mGeTuiPushInfoMap.put(messageId, new NotificationInfo(title, content, System.currentTimeMillis()));
            mPushHandler.sendMessageDelayed(message, 200);
            ZALog.i(TAG, "sendMessageDelayed,msgId = " + messageId);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    void trackReceiveMessageData(String sfDate, String msgId) {
        try {
            if (mPushHandler.hasMessages(GT_PUSH_MSG) && mGeTuiPushInfoMap.containsKey(msgId)) {
                mPushHandler.removeMessages(GT_PUSH_MSG);
                ZALog.i(TAG, "remove GeTui Push Message");
                NotificationInfo push = mGeTuiPushInfoMap.get(msgId);
                if (push != null) {
                    PushAutoTrackHelper.trackGeTuiNotificationClicked(push.title, push.content, sfDate, push.time);
                }
                mGeTuiPushInfoMap.remove(msgId);
                ZALog.i(TAG, " onGeTuiReceiveMessage:msg id : " + msgId);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private boolean isHooked(Intent intent) {
        try {
            return intent.hasExtra(ZA_PUSH_ID);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return false;
    }

    private void checkAndStoreNotificationInfo(PendingIntent pendingIntent, NotificationInfo info) {
        if (pendingIntent == null) {
            ZALog.i(TAG, "pendingIntent is null");
            return;
        }
        try {
            String intentId = this.mPendingIntent2Ids.get(pendingIntent);
            if (intentId != null) {
                storeNotificationInfo(info, intentId);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private void storeNotificationInfo(NotificationInfo push, String intentId) {
        ZALog.i(TAG, "storeNotificationInfo: id=" + intentId + ", actionInfo" + push);
        try {
            initAndCleanDir();
            File toFile = new File(this.mPushFile, intentId);
            if (toFile.exists()) {
                ZALog.i(TAG, "toFile exists");
                toFile.delete();
            }
            FileUtils.writeToFile(toFile, push.toJson());
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private synchronized void initAndCleanDir() {
        try {
            if (!this.mPushFile.exists()) {
                this.mPushFile.mkdirs();
            }
            File[] files = this.mPushFile.listFiles();
            if (files != null) {
                long currentTime = System.currentTimeMillis();
                for (File file : files) {
                    if (currentTime - file.lastModified() > 86400000) {
                        ZALog.i(TAG, "clean file: " + file.toString());
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private NotificationInfo getNotificationInfo(Notification notification) {
        NotificationInfo push = null;
        if (Build.VERSION.SDK_INT >= 19) {
            try {
                String title = notification.extras.getString("android.title");
                String content = notification.extras.getString("android.text");
                push = new NotificationInfo(title, content, 0L);
                ZALog.i(TAG, "NotificationInfo: title = " + title + "content = " + content);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
        return push;
    }

    private NotificationInfo getNotificationInfo(String id) {
        try {
            initAndCleanDir();
            File inFile = new File(this.mPushFile, id);
            if (!inFile.exists()) {
                return null;
            }
            String json = FileUtils.readFileToString(inFile);
            if (TextUtils.isEmpty(json)) {
                return null;
            }
            ZALog.i(TAG, "cache local notification info:" + json);
            return NotificationInfo.fromJson(json);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return null;
    }

    private void trackCustomizeClick(Intent intent) {
        if (!customizeEnable) {
            return;
        }
        try {
            if (isHooked(intent)) {
                final String id = intent.getStringExtra(ZA_PUSH_ID);
                intent.removeExtra(ZA_PUSH_ID);
                if (TextUtils.isEmpty(id)) {
                    ZALog.i(TAG, "intent tag is null");
                    return;
                }
                mPushHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        NotificationInfo push = getNotificationInfo(id);
                        if (push != null) {
                            PushAutoTrackHelper.trackNotificationOpenedEvent(null,
                                    push.title,
                                    push.content,
                                    "Local",
                                    null);
                        }
                    }
                });
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    static class NotificationInfo {
        String title;
        String content;
        long time;

        NotificationInfo(String title, String content, long time) {
            this.title = title;
            this.content = content;
            this.time = time;
        }

        public static NotificationInfo fromJson(String json) {
            try {
                JSONObject jsonObject = new JSONObject(json);
                return new NotificationInfo(jsonObject.optString("title"),
                        jsonObject.optString("content"),
                        jsonObject.optLong("time"));
            } catch (JSONException e) {
                ZALog.printStackTrace(e);
            }
            return null;
        }

        @Override
        public String toString() {
            return "NotificationInfo{" +
                    "title='" + title + '\'' +
                    ", content='" + content + '\'' +
                    ", time=" + time +
                    '}';
        }

        public String toJson() {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", title);
                jsonObject.put("content", content);
                jsonObject.put("time", time);
                return jsonObject.toString();
            } catch (JSONException e) {
                ZALog.printStackTrace(e);
            }
            return null;
        }
    }
}
