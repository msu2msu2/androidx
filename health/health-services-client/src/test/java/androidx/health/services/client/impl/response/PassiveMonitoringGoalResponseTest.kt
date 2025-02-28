/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.services.client.impl.response

import androidx.health.services.client.data.ComparisonType
import androidx.health.services.client.data.DataType.Companion.STEPS_DAILY
import androidx.health.services.client.data.DataTypeCondition
import androidx.health.services.client.data.PassiveGoal
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PassiveMonitoringGoalResponseTest {
    @Test
    fun protoRoundTrip() {
        val proto = PassiveMonitoringGoalResponse(
            PassiveGoal(
                DataTypeCondition(STEPS_DAILY, 1000, ComparisonType.GREATER_THAN),
                PassiveGoal.TriggerFrequency.REPEATED
            )
        ).proto

        val response = PassiveMonitoringGoalResponse(proto)

        assertThat(response.passiveGoal.dataTypeCondition.dataType).isEqualTo(STEPS_DAILY)
    }
}