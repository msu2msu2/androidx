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

package androidx.car.app.hardware.common;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Container class for information about property profile such as the car zones and supported
 * property values associated with them.
 *
 * <p>{@link PropertyManager} uses it to give response to front-end components such as
 * {@link androidx.car.app.hardware.climate.AutomotiveCarClimate}.
 *
 * @param <T> is the value type of response.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
@AutoValue
public abstract class CarPropertyProfile<T> {

    /** Returns one of the values in {@link android.car.VehiclePropertyIds}. */
    public abstract int getPropertyId();

    /** Returns a map of min/max values for a property corresponding to a set of car zones.
     *
     * <p>The set of car zones represent the zones in which the associated feature can be regulated
     * together.
     */
    @Nullable
    public abstract ImmutableMap<Set<CarZone>, Pair<T, T>> getCarZoneSetsToMinMaxRange();

    /** Returns a pair of min and max values for the temperature set in Celsius.
     *
     * <p> Not all the values within this range may be supported in the car.
     * If getCelsiusIncrement() returns a non-null value, then Min/Max values combined with the
     * Celsius increment can be used to determine the supported temperature values.</p>
     */
    @Nullable
    public abstract Pair<Float, Float> getCelsiusRange();

    /** Returns a pair of min and max values for the temperature set in Fahrenheit.
     *
     * <p> Not all the values within this range may be supported in the car.
     * If getFahrenheitRange() returns a non-null value, then Min/Max values combined with the
     * Fahrenheit increment can be used to determine the supported temperature values.</p>
     */

    @Nullable
    public abstract Pair<Float, Float> getFahrenheitRange();

    /** Returns the increment value for the temperature set config in Celsius. */
    public abstract float getCelsiusIncrement();

    /** Returns the increment value for the temperature set config in Fahrenheit. */
    public abstract float getFahrenheitIncrement();

    /** Returns a list of set of {@link CarZone}s controlled together. */
    @NonNull
    public abstract ImmutableList<Set<CarZone>> getCarZones();

    /** Returns one of the values in {@link CarValue.StatusCode}. */
    public abstract @CarValue.StatusCode int getStatus();

    /** Gets a builder class for {@link CarPropertyProfile}. */
    @NonNull
    public static <T> Builder<T> builder() {
        return new AutoValue_CarPropertyProfile.Builder<T>()
                .setCarZones(Collections.singletonList(
                        Collections.singleton(CarZone.CAR_ZONE_GLOBAL)))
                .setCarZoneSetsToMinMaxRange(null)
                .setCelsiusRange(null)
                .setFahrenheitRange(null)
                .setCelsiusIncrement(-1f)
                .setFahrenheitIncrement(-1f);
    }

    /**
     * A builder for {@link CarPropertyProfile}
     *
     * @param <T> is the type for all min/max values.
     */
    @AutoValue.Builder
    public abstract static class Builder<T> {
        /** Sets a property ID for the {@link CarPropertyProfile}. */
        @NonNull
        public abstract Builder<T> setPropertyId(int propertyId);

        /**
         * Sets a status code for the {@link CarPropertyProfile}.
         */
        @NonNull
        public abstract Builder<T> setStatus(@CarValue.StatusCode int status);

        /** Sets a min/max range pair value for the {@link CarPropertyProfile}. */
        @NonNull
        public abstract Builder<T> setCarZoneSetsToMinMaxRange(
                @Nullable Map<Set<CarZone>, Pair<T, T>> minMaxRange);

        /** Sets a min/max range for temperature in Celsius. */
        @NonNull
        public abstract Builder<T> setCelsiusRange(
                @Nullable Pair<Float, Float> celsiusRange);

        /** Sets a min/max range for temperature in Fahrenheit. */
        @NonNull
        public abstract Builder<T> setFahrenheitRange(
                @Nullable Pair<Float, Float> fahrenheitRange);

        /** Sets the value of increment for temperature set config in Celsius. */
        @NonNull
        public abstract Builder<T> setCelsiusIncrement(
                float celsiusIncrement);

        /** Sets the value of increment for temperature set config in Fahrenheit. */
        @NonNull
        public abstract Builder<T> setFahrenheitIncrement(
                float fahrenheitIncrement);

        /** Sets the list of set of {@link CarZone}s for the {@link CarPropertyProfile}. */
        @NonNull
        public abstract Builder<T> setCarZones(
                @NonNull List<Set<CarZone>> carZones);

        /** Creates an instance of {@link CarPropertyProfile}. */
        @NonNull
        public abstract CarPropertyProfile<T> build();
    }
}
