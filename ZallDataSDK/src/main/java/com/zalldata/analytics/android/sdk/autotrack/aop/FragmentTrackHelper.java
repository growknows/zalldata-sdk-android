/*
 * Created by guo on 2021/2/18.
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

package com.zalldata.analytics.android.sdk.autotrack.aop;

import android.os.Bundle;
import android.view.View;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.autotrack.ZAFragmentLifecycleCallbacks;
import com.zalldata.analytics.android.sdk.util.ZAFragmentUtils;

import java.util.HashSet;
import java.util.Set;

public class FragmentTrackHelper {
    // Fragment 的回调监听
    private static final Set<ZAFragmentLifecycleCallbacks> FRAGMENT_CALLBACKS = new HashSet<>();

    /**
     * 插件 Hook 处理 Fragment 的 onViewCreated 生命周期
     *
     * @param object Fragment
     * @param rootView View
     * @param bundle Bundle
     */
    public static void onFragmentViewCreated(Object object, View rootView, Bundle bundle) {
        if (!ZAFragmentUtils.isFragment(object)) {
            return;
        }
        for (ZAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onViewCreated(object, rootView, bundle);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 onResume 生命周期
     *
     * @param object Fragment
     */
    public static void trackFragmentResume(Object object) {
        if (!ZAFragmentUtils.isFragment(object)) {
            return;
        }
        for (ZAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onResume(object);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 onPause 生命周期
     *
     * @param object Fragment
     */
    public static void trackFragmentPause(Object object) {
        if (!ZAFragmentUtils.isFragment(object)) {
            return;
        }
        for (ZAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onPause(object);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 setUserVisibleHint 回调
     *
     * @param object Fragment
     * @param isVisibleToUser 是否可见
     */
    public static void trackFragmentSetUserVisibleHint(Object object, boolean isVisibleToUser) {
        if (!ZAFragmentUtils.isFragment(object)) {
            return;
        }
        for (ZAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.setUserVisibleHint(object, isVisibleToUser);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    /**
     * 插件 Hook 处理 Fragment 的 onHiddenChanged 回调
     *
     * @param object Fragment
     * @param hidden Fragment 是否隐藏
     */
    public static void trackOnHiddenChanged(Object object, boolean hidden) {
        if (!ZAFragmentUtils.isFragment(object)) {
            return;
        }
        for (ZAFragmentLifecycleCallbacks fragmentCallbacks : FRAGMENT_CALLBACKS) {
            try {
                fragmentCallbacks.onHiddenChanged(object, hidden);
            } catch (Exception e) {
                ZALog.printStackTrace(e);
            }
        }
    }

    /**
     * 添加 Fragment 的回调监听
     *
     * @param fragmentLifecycleCallbacks ZAFragmentLifecycleCallbacks
     */
    public static void addFragmentCallbacks(ZAFragmentLifecycleCallbacks fragmentLifecycleCallbacks) {
        FRAGMENT_CALLBACKS.add(fragmentLifecycleCallbacks);
    }

    /**
     * 移除指定的 Fragment 的回调监听
     *
     * @param fragmentLifecycleCallbacks ZAFragmentLifecycleCallbacks
     */
    public static void removeFragmentCallbacks(ZAFragmentLifecycleCallbacks fragmentLifecycleCallbacks) {
        FRAGMENT_CALLBACKS.remove(fragmentLifecycleCallbacks);
    }
}
