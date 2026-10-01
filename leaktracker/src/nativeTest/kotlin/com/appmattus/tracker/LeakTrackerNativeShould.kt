/**
 * Copyright 2017 Appmattus Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(NativeRuntimeApi::class, ExperimentalAtomicApi::class)

package com.appmattus.tracker

import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.native.runtime.GC
import kotlin.native.runtime.NativeRuntimeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.TimeSource

class LeakTrackerNativeShould {

    @Test
    fun execute_operation_when_unsubscriber_never_referenced() {
        val exceptions = AtomicInt(0)
        val unsubscribed = AtomicInt(0)
        val tracker = LeakTracker { exceptions.addAndFetch(1) }

        subscribeAndDiscard(tracker) { unsubscribed.addAndFetch(1) }

        val start = TimeSource.Monotonic.markNow()
        while (exceptions.load() == 0 && start.elapsedNow().inWholeSeconds < 10) {
            GC.collect()
        }

        assertEquals(1, exceptions.load())
        assertEquals(1, unsubscribed.load())
    }

    @Test
    fun not_execute_operation_when_unsubscribe_called() {
        val exceptions = AtomicInt(0)
        val tracker = LeakTracker { exceptions.addAndFetch(1) }

        tracker.subscribe { }.unsubscribe()

        GC.collect()
        GC.collect()

        assertEquals(0, exceptions.load())
    }

    private fun subscribeAndDiscard(tracker: LeakTracker, operation: () -> Unit) {
        tracker.subscribe(operation)
    }
}
