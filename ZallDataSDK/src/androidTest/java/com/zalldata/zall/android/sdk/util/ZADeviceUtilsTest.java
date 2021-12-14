package com.zalldata.zall.android.sdk.util;

import android.Manifest;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import com.zalldata.zall.android.sdk.ZAConfigOptions;
import com.zalldata.zall.android.sdk.ZallDataAPI;
import com.zalldata.zall.android.sdk.advert.utils.OaidHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class ZADeviceUtilsTest {
    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.READ_PHONE_STATE);

    @Before
    public void initZallDataAPI() {
        Context context = ApplicationProvider.getApplicationContext();
        ZallDataAPI.sharedInstance(context, new ZAConfigOptions("").enableLog(true));
    }

    /**
     * 需集成 oaid 的 aar 包
     */
    @Test
    public void getOAID() {
        try {
            String oaid = OaidHelper.getOAID(ApplicationProvider.getApplicationContext());
            assertNull(oaid);
            ZallDataAPI.sharedInstance().trackInstallation("AppInstall");
        } catch (Exception ex) {
            //ignore
        }
    }
}
