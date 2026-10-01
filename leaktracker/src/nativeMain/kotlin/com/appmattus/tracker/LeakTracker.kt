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

@file:OptIn(ExperimentalNativeApi::class, ExperimentalAtomicApi::class)

package com.appmattus.tracker

import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.ref.Cleaner
import kotlin.native.ref.createCleaner

/**
 * Notifies you when the referent object is garbage collected
 */
public actual class LeakTracker actual constructor(private val exceptionHandler: (Exception) -> Unit) {

    /**
     * Tracks the Unsubscriber returned, calling the provided exception handler if unsubscribe() is not called before
     * the Unsubscriber is garbage collected
     *
     * @param unsubscribeOperation  the operation to execute when unsubscribe() is called
     */
    public actual fun subscribe(unsubscribeOperation: () -> Unit): Unsubscriber {
        val exception = IllegalStateException("Subscription has not been un-subscribed")
        val unsubscribed = AtomicBoolean(false)

        // The cleaner resource must not reference the unsubscriber otherwise it would never be collected
        val resource = LeakResource(unsubscribed, exceptionHandler, exception, unsubscribeOperation)
        val cleaner = createCleaner(resource) {
            if (it.unsubscribed.compareAndSet(expectedValue = false, newValue = true)) {
                it.exceptionHandler(it.exception)
                it.unsubscribeOperation()
            }
        }

        return NativeUnsubscriber(unsubscribed, unsubscribeOperation, cleaner)
    }

    private class LeakResource(
        val unsubscribed: AtomicBoolean,
        val exceptionHandler: (Exception) -> Unit,
        val exception: Exception,
        val unsubscribeOperation: () -> Unit
    )

    private class NativeUnsubscriber(
        private val unsubscribed: AtomicBoolean,
        private val unsubscribeOperation: () -> Unit,
        @Suppress("unused") private val cleaner: Cleaner
    ) : Unsubscriber {
        override fun unsubscribe() {
            unsubscribed.store(true)
            unsubscribeOperation()
        }
    }
}
