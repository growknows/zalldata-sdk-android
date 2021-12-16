/*
 * Created by guo on 2020/3/20.
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

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.zalldata.analytics.android.sdk.ZALog;
import com.zalldata.analytics.android.sdk.advert.utils.ChannelUtils;
import com.zalldata.analytics.android.sdk.util.ZallDataUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


class ChannelDeepLink extends AbsDeepLink {

    ChannelDeepLink(Intent intent) {
        super(intent);
    }

    @Override
    public void parseDeepLink(Intent intent) {
        if (intent == null || intent.getData() == null) {
            return;
        }
        Uri uri = intent.getData();
        if (uri.isOpaque()) {
            ZALog.d("ChannelDeepLink", uri.toString() + " isOpaque");
            return;
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            return;
        }
        Set<String> parameterNames = null;
        parameterNames = uri.getQueryParameterNames();
        if (parameterNames != null && parameterNames.size() > 0) {
            Map<String, String> uriParams = new HashMap<>();
            for (String name : parameterNames) {
                String value = uri.getQueryParameter(name);
                uriParams.put(name, TextUtils.isEmpty(value) ? "" : value);
            }
            ChannelUtils.parseParams(uriParams);
            if (mCallBack != null) {
                mCallBack.onFinish(DeepLinkManager.DeepLinkType.CHANNEL, null, true, 0);
            }
        }
    }

    @Override
    public void mergeDeepLinkProperty(JSONObject properties) {
        try {
            properties.put("$deeplink_url", getDeepLinkUrl());
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
        ZallDataUtils.mergeJSONObject(ChannelUtils.getUtmProperties(), properties);
    }
}
