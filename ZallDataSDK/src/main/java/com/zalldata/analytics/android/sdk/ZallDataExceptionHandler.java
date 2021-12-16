/*
 * Created by guo on 2021/7/24.
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

package com.zalldata.analytics.android.sdk;


import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

public class ZallDataExceptionHandler implements Thread.UncaughtExceptionHandler {
    private static final int SLEEP_TIMEOUT_MS = 500;
    private static final ArrayList<ZAExceptionListener> sExceptionListeners = new ArrayList<>();
    private static ZallDataExceptionHandler sInstance;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private static boolean isTrackCrash = false;

    private ZallDataExceptionHandler() {
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    synchronized static void init() {
        if (sInstance == null) {
            sInstance = new ZallDataExceptionHandler();
        }
    }

    static void addExceptionListener(ZAExceptionListener listener) {
        sExceptionListeners.add(listener);
    }

    static void enableAppCrash() {
        isTrackCrash = true;
    }

    @Override
    public void uncaughtException(final Thread t, final Throwable e) {
        try {
            if (isTrackCrash) {
                try {
                    final JSONObject messageProp = new JSONObject();
                    try {
                        Writer writer = new StringWriter();
                        PrintWriter printWriter = new PrintWriter(writer);
                        e.printStackTrace(printWriter);
                        Throwable cause = e.getCause();
                        while (cause != null) {
                            cause.printStackTrace(printWriter);
                            cause = cause.getCause();
                        }
                        printWriter.close();
                        String result = writer.toString();
                        messageProp.put("app_crashed_reason", result);
                    } catch (Exception ex) {
                        ZALog.printStackTrace(ex);
                    }
                    ZallDataAPI.sharedInstance().trackEvent(EventType.TRACK, "AppCrashed", messageProp, null);
                } catch (Exception ex) {
                    ZALog.printStackTrace(ex);
                }
            }

            for (ZAExceptionListener exceptionListener : sExceptionListeners) {
                try {
                    exceptionListener.uncaughtException(t, e);
                } catch (Exception e1) {
                    ZALog.printStackTrace(e1);
                }
            }
            ZallDataAPI.sharedInstance().flush();
            try {
                Thread.sleep(SLEEP_TIMEOUT_MS);
            } catch (InterruptedException e1) {
                ZALog.printStackTrace(e1);
            }
            if (mDefaultExceptionHandler != null) {
                mDefaultExceptionHandler.uncaughtException(t, e);
            } else {
                killProcessAndExit();
            }
        } catch (Exception exception) {
            //ignored
        }
    }

    private void killProcessAndExit() {
        try {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(10);
        } catch (Exception e) {
            //ignored
        }
    }

    /**
     * 异常监听回调
     */
    public interface ZAExceptionListener {
        void uncaughtException(final Thread t, final Throwable e);
    }
}
