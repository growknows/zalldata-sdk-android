/*
 * Created by guo on 2020/6/22.
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

public interface ThreadNameConstants {
    String THREAD_TASK_QUEUE = "ZA.TaskQueueThread";
    String THREAD_TASK_EXECUTE = "ZA.TaskExecuteThread";
    String THREAD_SEND_DISTINCT_ID = "ZA.SendDistinctIDThread";
    String THREAD_DEEP_LINK_REQUEST = "ZA.DeepLinkRequest";
    String THREAD_PUSH_HANDLER = "ZA.PushThread";
}
