/*
 * Created by guo on 2020/5/4.
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

import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.ZallDataAPI;
import com.zalldata.analytics.android.sdk.ZallDataExceptionHandler;
import com.zalldata.analytics.android.sdk.autotrack.utils.AutoTrackUtils;
import com.zalldata.analytics.android.sdk.util.AopUtil;
import com.zalldata.analytics.android.sdk.util.ZAFragmentUtils;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;
import com.zalldata.analytics.android.sdk.util.TimeUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Fragment 页面停留时长监听
 */
public class FragmentPageLeaveCallbacks implements ZAFragmentLifecycleCallbacks, ZallDataExceptionHandler.ZAExceptionListener {
    private final HashMap<Integer, JSONObject> mResumedFragments = new HashMap<>();
    private static final String START_TIME = "za_start_time";

    @Override
    public void onCreate(Object object) {

    }

    @Override
    public void onViewCreated(Object object, View rootView, Bundle bundle) {

    }

    @Override
    public void onStart(Object object) {

    }

    @Override
    public void onResume(Object object) {
        if (ZAFragmentUtils.isFragmentVisible(object)) {
            trackFragmentStart(object);
        }
    }

    @Override
    public void onPause(Object object) {
        try {
            int hashCode = object.hashCode();
            if (mResumedFragments.containsKey(hashCode)) {
                trackAppPageLeave(object);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public void onStop(Object object) {

    }

    @Override
    public void onHiddenChanged(Object object, boolean hidden) {
        if (ZAFragmentUtils.isFragmentVisible(object)) {
            trackFragmentStart(object);
        } else {
            trackAppPageLeave(object);
        }
    }

    @Override
    public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (ZAFragmentUtils.isFragmentVisible(object)) {
            trackFragmentStart(object);
        } else {
            trackAppPageLeave(object);
        }
    }

    private void trackAppPageLeave(Object object) {
        try {
            int hashCode = object.hashCode();
            if (mResumedFragments.containsKey(hashCode)) {
                JSONObject properties = mResumedFragments.get(hashCode);
                mResumedFragments.remove(hashCode);
                if (properties != null) {
                    trackPageLeaveEvent(properties);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private void trackFragmentStart(Object object) {
        try {
            JSONObject properties = new JSONObject();
            properties.put(START_TIME, SystemClock.elapsedRealtime());
            String url = ZallDataUtils.getScreenUrl(object);
            properties.put("$url", url);
            String referrer = AutoTrackUtils.getLastScreenUrl();
            if (!TextUtils.isEmpty(referrer)) {
                properties.put("$referrer", referrer);
            }
            AopUtil.getScreenNameAndTitleFromFragment(properties, object, null);
            mResumedFragments.put(object.hashCode(), properties);
            AutoTrackUtils.setLastScreenUrl(url);
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        try {
            Iterator<Integer> keyCodes = mResumedFragments.keySet().iterator();
            while (keyCodes.hasNext()) {
                int hashCode = keyCodes.next();
                JSONObject properties = mResumedFragments.get(hashCode);
                if (properties == null) {
                    continue;
                }
                trackPageLeaveEvent(properties);
                keyCodes.remove();
            }
        } catch (Exception exception) {
            ZALog.printStackTrace(exception);
        }
    }

    private void trackPageLeaveEvent(JSONObject properties) {
        try {
            long resumeTime = properties.optLong(START_TIME);
            properties.remove(START_TIME);
            double duration = TimeUtils.duration(resumeTime, SystemClock.elapsedRealtime());
            if (duration < 0.05) {
                return;
            }
            properties.put("event_duration", duration);
            ZallDataAPI.sharedInstance().trackInternal("$AppPageLeave", properties);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }
}
