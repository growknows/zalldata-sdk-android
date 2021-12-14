/*
 * Created by guo on 2021/10/7.
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

package com.zalldata.zall.android.sdk.autotrack;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.zalldata.zall.android.sdk.AopConstants;
import com.zalldata.zall.android.sdk.AppStateManager;
import com.zalldata.zall.android.sdk.R;
import com.zalldata.zall.android.sdk.ZALog;
import com.zalldata.zall.android.sdk.ScreenAutoTracker;
import com.zalldata.zall.android.sdk.ZallDataAPI;
import com.zalldata.zall.android.sdk.util.AopUtil;
import com.zalldata.zall.android.sdk.util.ZADataHelper;
import com.zalldata.zall.android.sdk.util.ZAFragmentUtils;
import com.zalldata.zall.android.sdk.util.ZallDataUtils;
import com.zalldata.zall.android.sdk.util.WeakSet;

import org.json.JSONObject;

import java.util.Set;

/**
 * Fragment 的页面浏览
 */
public class FragmentViewScreenCallbacks implements ZAFragmentLifecycleCallbacks {

    private final static String TAG = "ZA.FragmentViewScreenCallbacks";
    private final Set<Object> mPageFragments = new WeakSet<>();

    @Override
    public void onCreate(Object object) {

    }

    @Override
    public void onViewCreated(Object object, View rootView, Bundle bundle) {
        try {
            //Fragment名称
            String fragmentName = object.getClass().getName();
            rootView.setTag(R.id.zall_data_tag_view_fragment_name, fragmentName);

            if (rootView instanceof ViewGroup) {
                traverseView(fragmentName, (ViewGroup) rootView);
            }

            //获取所在的 Context
            Context context = rootView.getContext();
            //将 Context 转成 Activity
            Activity activity = AopUtil.getActivityFromContext(context, rootView);
            if (activity != null) {
                Window window = activity.getWindow();
                if (window != null) {
                    window.getDecorView().getRootView().setTag(R.id.zall_data_tag_view_fragment_name, "");
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public void onStart(Object object) {

    }

    @Override
    public void onResume(Object object) {
        try {
            if (isFragmentValid(object)) {
                trackFragmentAppViewScreen(object);
                mPageFragments.add(object);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public void onPause(Object object) {
        if (object != null) {
            mPageFragments.remove(object);
        }
    }

    @Override
    public void onStop(Object object) {

    }

    @Override
    public void onHiddenChanged(Object object, boolean hidden) {
        try {
            if (object == null) {
                ZALog.d(TAG, "fragment is null,return");
                return;
            }
            if (hidden) {
                mPageFragments.remove(object);
                ZALog.d(TAG, "fragment hidden is true,return");
                return;
            }
            if (isFragmentValid(object)) {
                trackFragmentAppViewScreen(object);
                mPageFragments.add(object);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    @Override
    public void setUserVisibleHint(Object object, boolean isVisibleToUser) {
        try {
            if (object == null) {
                ZALog.d(TAG, "object is null");
                return;
            }
            if (!isVisibleToUser) {
                mPageFragments.remove(object);
                ZALog.d(TAG, "fragment isVisibleToUser is false,return");
                return;
            }
            if (isFragmentValid(object)) {
                trackFragmentAppViewScreen(object);
                mPageFragments.add(object);
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private void trackFragmentAppViewScreen(Object fragment) {
        try {
            JSONObject properties = new JSONObject();
            AopUtil.getScreenNameAndTitleFromFragment(properties, fragment, null);
            AppStateManager.getInstance().setFragmentScreenName(fragment, properties.optString(AopConstants.SCREEN_NAME));
            if (fragment instanceof ScreenAutoTracker) {
                ScreenAutoTracker screenAutoTracker = (ScreenAutoTracker) fragment;
                JSONObject otherProperties = screenAutoTracker.getTrackProperties();
                if (otherProperties != null) {
                    ZallDataUtils.mergeJSONObject(otherProperties, properties);
                }
            }
            JSONObject eventProperties = ZADataHelper.appendLibMethodAutoTrack(properties);
            ZallDataAPI.sharedInstance().trackViewScreen(ZallDataUtils.getScreenUrl(fragment), eventProperties);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    private boolean isFragmentValid(Object fragment) {
        if (fragment == null) {
            ZALog.d(TAG, "fragment is null,return");
            return false;
        }

        if (ZallDataAPI.sharedInstance().isAutoTrackEventTypeIgnored(ZallDataAPI.AutoTrackEventType.APP_VIEW_SCREEN)) {
            ZALog.d(TAG, "AutoTrackEventTypeIgnored,return");
            return false;
        }

        if (!ZallDataAPI.sharedInstance().isTrackFragmentAppViewScreenEnabled()) {
            ZALog.d(TAG, "TrackFragmentAppViewScreenEnabled is false,return");
            return false;
        }

        if ("com.bumptech.glide.manager.SupportRequestManagerFragment".equals(fragment.getClass().getCanonicalName())) {
            ZALog.d(TAG, "fragment is SupportRequestManagerFragment,return");
            return false;
        }

        boolean isAutoTrackFragment = ZallDataAPI.sharedInstance().isFragmentAutoTrackAppViewScreen(fragment.getClass());
        if (!isAutoTrackFragment) {
            ZALog.d(TAG, "fragment class ignored,return");
            return false;
        }
        //针对主动调用 fragment 生命周期，重复触发浏览
        if (mPageFragments.contains(fragment)) {
            ZALog.d(TAG, "pageFragment contains,return");
            return false;
        }
        if (!ZAFragmentUtils.isFragmentVisible(fragment)) {
            ZALog.d(TAG, "fragment is not visible,return");
            return false;
        }
        return true;
    }

    private static void traverseView(String fragmentName, ViewGroup root) {
        try {
            if (TextUtils.isEmpty(fragmentName) || root == null) {
                return;
            }
            final int childCount = root.getChildCount();
            for (int i = 0; i < childCount; ++i) {
                final View child = root.getChildAt(i);
                child.setTag(R.id.zall_data_tag_view_fragment_name, fragmentName);
                if (child instanceof ViewGroup && !(child instanceof ListView ||
                        child instanceof GridView ||
                        child instanceof Spinner ||
                        child instanceof RadioGroup)) {
                    traverseView(fragmentName, (ViewGroup) child);
                }
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }
}
