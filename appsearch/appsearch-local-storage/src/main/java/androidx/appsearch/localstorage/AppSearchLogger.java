/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.appsearch.localstorage;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.localstorage.stats.CallStats;
import androidx.appsearch.localstorage.stats.InitializeStats;
import androidx.appsearch.localstorage.stats.OptimizeStats;
import androidx.appsearch.localstorage.stats.PutDocumentStats;
import androidx.appsearch.localstorage.stats.RemoveStats;
import androidx.appsearch.localstorage.stats.SearchStats;
import androidx.appsearch.localstorage.stats.SetSchemaStats;

/**
 * An interface for implementing client-defined logging AppSearch operations stats.
 *
 * <p>Any implementation needs to provide general information on how to log all the stats types.
 * (e.g. {@link CallStats})
 *
 * <p>All implementations of this interface must be thread safe.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface AppSearchLogger {
    /**
     * Logs {@link CallStats}
     */
    void logStats(@NonNull CallStats stats);

    /**
     * Logs {@link PutDocumentStats}
     */
    void logStats(@NonNull PutDocumentStats stats);

    /**
     * Logs {@link InitializeStats}
     */
    void logStats(@NonNull InitializeStats stats);

    /**
     * Logs {@link SearchStats}
     */
    void logStats(@NonNull SearchStats stats);

    /**
     * Logs {@link RemoveStats}
     */
    void logStats(@NonNull RemoveStats stats);

    /**
     * Logs {@link OptimizeStats}
     */
    void logStats(@NonNull OptimizeStats stats);

    /**
     * Logs {@link SetSchemaStats}
     */
    void logStats(@NonNull SetSchemaStats stats);

    // TODO(b/173532925) Add remaining logStats once we add all the stats.
}
