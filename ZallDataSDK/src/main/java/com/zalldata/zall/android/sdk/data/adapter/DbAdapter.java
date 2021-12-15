/*
 * Created by guo on 2020/5/25.
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

package com.zalldata.zall.android.sdk.data.adapter;

import android.content.ContentValues;
import android.content.Context;

import com.zalldata.zall.android.sdk.ZALog;
import com.zalldata.zall.android.sdk.encrypt.ZallDataEncrypt;

import org.json.JSONException;
import org.json.JSONObject;

public class DbAdapter {
    private static DbAdapter instance;
    private final DbParams mDbParams;
    private DataOperation mTrackEventOperation;
    private DataOperation mPersistentOperation;

    private DbAdapter(Context context, String packageName, ZallDataEncrypt zallDataEncrypt) {
        mDbParams = DbParams.getInstance(packageName);
        if (zallDataEncrypt != null) {
            mTrackEventOperation = new EncryptDataOperation(context.getApplicationContext(), zallDataEncrypt);
        } else {
            mTrackEventOperation = new EventDataOperation(context.getApplicationContext());
        }
        mPersistentOperation = new PersistentDataOperation(context.getApplicationContext());
    }

    public static DbAdapter getInstance(Context context, String packageName,
                                        ZallDataEncrypt zallDataEncrypt) {
        if (instance == null) {
            instance = new DbAdapter(context, packageName, zallDataEncrypt);
        }
        return instance;
    }

    public static DbAdapter getInstance() {
        if (instance == null) {
            throw new IllegalStateException("The static method getInstance(Context context, String packageName) should be called before calling getInstance()");
        }
        return instance;
    }

    /**
     * Adds a JSON string representing an event with properties or a person record
     * to the SQLiteDatabase.
     *
     * @param j the JSON to record
     * @return the number of rows in the table, or DB_OUT_OF_MEMORY_ERROR/DB_UPDATE_ERROR
     * on failure
     */
    public int addJSON(JSONObject j) {
        int code = mTrackEventOperation.insertData(mDbParams.getEventUri(), j);
        if (code == 0) {
            return mTrackEventOperation.queryDataCount(mDbParams.getEventUri());
        }
        return code;
    }

    /**
     * Removes all events from table
     */
    public void deleteAllEvents() {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), DbParams.DB_DELETE_ALL);
    }

    /**
     * Removes events with an _id &lt;= last_id from table
     *
     * @param last_id the last id to delete
     * @return the number of rows in the table
     */
    public int cleanupEvents(String last_id) {
        mTrackEventOperation.deleteData(mDbParams.getEventUri(), last_id);
        return mTrackEventOperation.queryDataCount(mDbParams.getEventUri());
    }

    /**
     * 保存启动的页面个数
     *
     * @param activityCount 页面个数
     */
    public void commitActivityCount(int activityCount) {
        try {
            mPersistentOperation.insertData(mDbParams.getActivityStartCountUri(), new JSONObject().put(DbParams.VALUE, activityCount));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取存储的页面个数
     *
     * @return 存储的页面个数
     */
    public int getActivityCount() {
        String[] values = mPersistentOperation.queryData(mDbParams.getActivityStartCountUri(), 1);
        if (values != null && values.length > 0) {
            return Integer.parseInt(values[0]);
        }
        return 0;
    }

    /**
     * 设置 Activity Start 的时间戳
     *
     * @param appStartTime Activity Start 的时间戳
     */
    public void commitAppStartTime(long appStartTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppStartTimeUri(), new JSONObject().put(DbParams.VALUE, appStartTime));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity Start 的时间戳
     *
     * @return Activity Start 的时间戳
     */
    public long getAppStartTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getAppStartTimeUri(), 1);
            if (values != null && values.length > 0) {
                return Long.parseLong(values[0]);
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 设置 Activity End 的信息
     *
     * @param appEndData Activity End 的信息
     */
    public void commitAppEndData(String appEndData) {
        try {
            mPersistentOperation.insertData(mDbParams.getAppEndDataUri(), new JSONObject().put(DbParams.VALUE, appEndData));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Activity End 的信息
     *
     * @return Activity End 的信息
     */
    public String getAppEndData() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getAppEndDataUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 存储 LoginId
     *
     * @param loginId 登录 Id
     */
    public void commitLoginId(String loginId) {
        try {
            mPersistentOperation.insertData(mDbParams.getLoginIdUri(), new JSONObject().put(DbParams.VALUE, loginId));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取 LoginId
     *
     * @return LoginId
     */
    public String getLoginId() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getLoginIdUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 设置 Session 的时长
     *
     * @param sessionIntervalTime Session 的时长
     */
    public void commitSessionIntervalTime(int sessionIntervalTime) {
        try {
            mPersistentOperation.insertData(mDbParams.getSessionTimeUri(), new JSONObject().put(DbParams.VALUE, sessionIntervalTime));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取 Session 的时长
     *
     * @return Session 的时长
     */
    public int getSessionIntervalTime() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getSessionTimeUri(), 1);
            if (values != null && values.length > 0) {
                return Integer.parseInt(values[0]);
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return 0;
    }

    /**
     * 查询表中是否有对应的事件
     *
     * @param eventName 事件名
     * @return false 表示已存在，true 表示不存在，是首次
     */
    public boolean isFirstChannelEvent(String eventName) {
        try {
            return mTrackEventOperation.queryDataCount(mDbParams.getChannelPersistentUri(), null, DbParams.KEY_CHANNEL_EVENT_NAME + " = ? ", new String[]{eventName}, null) <= 0;
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return false;
    }

    /**
     * 添加渠道事件
     *
     * @param eventName 事件名
     */
    public void addChannelEvent(String eventName) {
        try {
            ContentValues values = new ContentValues();
            values.put(DbParams.KEY_CHANNEL_EVENT_NAME, eventName);
            values.put(DbParams.KEY_CHANNEL_RESULT, true);
            mTrackEventOperation.insertData(mDbParams.getChannelPersistentUri(), values);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 保存子进程上报数据的状态
     *
     * @param flushState 上报状态
     */
    public void commitSubProcessFlushState(boolean flushState) {
        try {
            mPersistentOperation.insertData(mDbParams.getSubProcessUri(), new JSONObject().put(DbParams.VALUE, flushState));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取子进程上报数据状态
     *
     * @return 上报状态
     */
    public boolean isSubProcessFlushing() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getSubProcessUri(), 1);
            if (values != null && values.length > 0) {
                return Integer.parseInt(values[0]) == 1;
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return true;
    }

    /**
     * 保存首个启动进程的标记
     *
     * @param isFirst 是否首个进程
     */
    public void commitFirstProcessState(boolean isFirst) {
        try {
            mPersistentOperation.insertData(mDbParams.getFirstProcessUri(), new JSONObject().put(DbParams.VALUE, isFirst));
        } catch (JSONException e) {
            ZALog.printStackTrace(e);
        }
    }

    /**
     * 获取是否首个启动进程的标记
     *
     * @return 是否首个进程
     */
    public boolean isFirstProcess() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getFirstProcessUri(), 1);
            if (values != null && values.length > 0) {
                return Integer.parseInt(values[0]) == 1;
            }
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
        return true;
    }

    /**
     * 保存远程控制下发字段
     *
     * @param config 下发字段
     */
    public void commitRemoteConfig(String config) {
        try {
            mPersistentOperation.insertData(mDbParams.getRemoteConfigUri(), new JSONObject().put(DbParams.VALUE, config));
        } catch (Exception ex) {
            ZALog.printStackTrace(ex);
        }
    }

    /**
     * 获取远程控制下发字段
     *
     * @return 下发字段
     */
    public String getRemoteConfig() {
        try {
            String[] values = mPersistentOperation.queryData(mDbParams.getRemoteConfigUri(), 1);
            if (values != null && values.length > 0) {
                return values[0];
            }
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return "";
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param tableName 表名
     * @param limit 条数限制
     * @return 数据
     */
    public String[] generateDataString(String tableName, int limit) {
        try {
            return mTrackEventOperation.queryData(mDbParams.getEventUri(), limit);
        } catch (Exception e) {
            ZALog.printStackTrace(e);
        }
        return null;
    }
}