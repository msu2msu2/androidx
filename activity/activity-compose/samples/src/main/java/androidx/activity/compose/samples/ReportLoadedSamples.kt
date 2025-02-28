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
package androidx.activity.compose.samples

import androidx.activity.compose.ReportLoadedAfter
import androidx.activity.compose.ReportLoadedWhen
import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Sampled
@Composable
fun ReportLoadedWhenSample() {
    var contentComposed by remember { mutableStateOf(false) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(100) {
            contentComposed = true
            Text("Hello World $it")
        }
    }
    ReportLoadedWhen { contentComposed }
}

@Sampled
@Composable
fun ReportLoadedAfterSample() {
    val lazyListState = remember { LazyListState() }
    ReportLoadedAfter {
        lazyListState.animateScrollToItem(10)
    }
}