/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appcompat.app

import androidx.appcompat.app.LocalesLateOnCreateActivity.DEFAULT_LOCALE_LIST
import androidx.appcompat.app.LocalesLateOnCreateActivity.EXPECTED_LOCALE_LIST
import androidx.appcompat.app.LocalesLateOnCreateActivity.TEST_LOCALE_LIST
import androidx.appcompat.testutils.LocalesActivityTestRule
import androidx.appcompat.testutils.LocalesUtils.assertConfigurationLocalesEquals
import androidx.appcompat.testutils.LocalesUtils.setLocalesAndWaitForRecreate
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.testutils.LifecycleOwnerUtils.waitUntilState
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(maxSdkVersion = 32)
class LocalesLateOnCreateTestCase {

    @get:Rule
    val activityRule = LocalesActivityTestRule(LocalesLateOnCreateActivity::class.java)

    @Ignore("b/241547343") // Test is 100% failing across multiple API levels.
    @Test
    fun testActivityRecreateLoop() {
        // Activity should be able to reach fully resumed state in default locales.
        waitUntilState(activityRule.activity, Lifecycle.State.RESUMED)
        assertConfigurationLocalesEquals(
            DEFAULT_LOCALE_LIST,
            activityRule.activity.resources.configuration
        )

        // Simulate the user set locales, which should force an activity recreate().
        setLocalesAndWaitForRecreate(
            activityRule,
            TEST_LOCALE_LIST
        )

        // Activity should be able to reach fully resumed state again.
        waitUntilState(activityRule.activity, Lifecycle.State.RESUMED)

        // The requested locales should have been set during attachBaseContext().
        assertConfigurationLocalesEquals(
            EXPECTED_LOCALE_LIST,
            activityRule.activity.resources.configuration
        )
    }
}
