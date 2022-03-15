/*
 * Created by guo on 2019/04/17.
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

package com.zalldata.analytics.android.demo.activity

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.jpush.android.api.JPushInterface
import com.huawei.hms.aaid.HmsInstanceId
import com.huawei.hms.common.ApiException
import com.zalldata.analytics.android.demo.PopupMenuActivity
import com.zalldata.analytics.android.demo.R
import com.zalldata.analytics.android.demo.custom.HorizonRecyclerDivider
import com.zalldata.analytics.android.sdk.util.ZallDataUtils
import kotlinx.android.synthetic.main.activity_test_list.*
import java.security.AccessController.getContext

class TestMainActivity : AppCompatActivity() {
    private lateinit var testListAdapter: TestMainAdapter
    private lateinit var dataList: List<DataEntity>
    private val TAG = "TestMainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_list)
        dataList = listOf(DataEntity("通用 Property 和基础接口设置", BasePropertyActivity::class.java, true),
                DataEntity("track*, profile* 等接口调用", TrackProfileSettingsActivity::class.java, true),
                DataEntity("OnClick", ClickActivity::class.java, true),
                DataEntity("H5 页面测试", H5Activity::class.java, true),
                DataEntity("可视化内嵌 H5", H5VisualTestActivity::class.java, true),
                DataEntity("Widget 采集测试", WidgetTestActivity::class.java, true),
                DataEntity("ViewPager & Fragment 测试", FragmentActivity::class.java, true),
                DataEntity("TabHost", MyTabHostActivity::class.java, true),
                DataEntity("NavigationView", NavigationViewActivity::class.java, true),
                DataEntity("ViewScreen", ViewScreenActivity::class.java, true),
                DataEntity("ListView & ExpandableListView", ListViewTestActivity::class.java, true),
                DataEntity("GridView ", GridViewTestActivity::class.java, true),
                DataEntity("hint 采集", HintTestActivity::class.java, true),
                DataEntity("Crash 测试", CrashTestActivity::class.java, true),
                DataEntity("PopupMenu 测试", PopupMenuActivity::class.java, true),
                DataEntity("Dialog", DialogActivity::class.java, true),
                DataEntity("黑名单白名单", BaseActivity::class.java, false),
                DataEntity("Debug 模式", BaseActivity::class.java, false),
                DataEntity("点击图 HeatMap", BaseActivity::class.java),
                DataEntity("可视化全埋点", BaseActivity::class.java),
                DataEntity("ListView 内嵌", InnerListTestActivity::class.java, true),
                DataEntity("ActionBar && ToolBar", ActionBarAndToolBarTestActivity::class.java, true),
                DataEntity("Lambda 点击事件", LambdaTestPageActivity::class.java, true)
        )
        recyclerView.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        recyclerView.addItemDecoration(HorizonRecyclerDivider(this, HorizonRecyclerDivider.VERTICAL_LIST))
        testListAdapter = TestMainAdapter(this, dataList)
        recyclerView.adapter = testListAdapter
        getToken()



    }

    class DataEntity(val content: String, val activityClazz: Class<*>, val isSupported: Boolean = false)

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        ZallDataUtils.handleSchemeUrl(this, intent)
    }
    private fun getToken() {
        // 创建一个新线程
        object : Thread() {
            override fun run() {
                try {
                    // 从agconnect-services.json文件中读取APP_ID
                    val appId = "105761465"

                    // 输入token标识"HCM"
                    val tokenScope = "HCM"
                    val token = HmsInstanceId.getInstance(this@TestMainActivity).getToken(appId, tokenScope)
                    Log.i(TAG, "get token:$token")

                    // 判断token是否为空
                    if (!TextUtils.isEmpty(token)) {
                        sendRegTokenToServer(token)
                    }
                } catch (e: ApiException) {
                    Log.e(TAG, "get token failed, $e")
                }
            }
        }.start()
    }
    private fun sendRegTokenToServer(token: String?) {
        Log.i(TAG, "sending token to server. token:$token")
    }


}