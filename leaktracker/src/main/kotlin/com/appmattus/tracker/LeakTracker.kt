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

package com.appmattus.tracker

import androidx.annotation.CheckResult
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Notifies you when the referent object is garbage collected
 */
public class LeakTracker(private val exceptionHandler: (Exception) -> Unit) {
    private val trackers: MutableMap<UUID, TrackedReference> = Collections.synchronizedMap(mutableMapOf())
    private val referenceQueue: ReferenceQueue<Any> = ReferenceQueue()

    init {
        // Start the reaper thread. Must happen after the referenceQueue above is created
        startReaperThread()
    }

    /**
     * Tracks the Unsubscriber returned, calling the provided exception handler if unsubscribe() is not called before
     * the Unsubscriber is garbage collected
     *
     * @param unsubscribeOperation  the operation to execute when unsubscribe() is called
     */
    @CheckResult
    public fun subscribe(unsubscribeOperation: () -> Unit): Unsubscriber {
        // generate exception and cleanup stack trace to remove this class
        val exception = IllegalStateException("Subscription has not been un-subscribed")
        exception.stackTrace = exception.stackTrace.drop(1).toTypedArray()

        val uuid = UUID.randomUUID()

        return object : Unsubscriber {
            override fun unsubscribe() {
                trackers.remove(uuid)?.clear()
                unsubscribeOperation()
            }
        }.apply {
            trackers[uuid] = TrackedReference(uuid, this) {
                exceptionHandler(exception)
                unsubscribeOperation()
            }
        }
    }

    /**
     * A weak reference to the referent object that will be added to the referenceQueue when the referent is garbage
     * collected.
     *
     * @param uuid      unique identifier for this reference
     * @param referent  object the new weak reference will refer to
     * @param operation the operation to execute when the referent is garbage collected
     */
    private inner class TrackedReference(val uuid: UUID, referent: Unsubscriber, val operation: () -> Unit) :
        WeakReference<Any>(referent, referenceQueue)

    @Suppress("TooGenericExceptionCaught", "EmptyCatchBlock")
    private fun startReaperThread() {
        // This thread must be a daemon thread otherwise it will stop the app finishing
        GlobalScope.launch(Executors.newSingleThreadExecutor().asCoroutineDispatcher()) {
            while (true) {
                try {
                    // Once a referent is to be GC'd then the Tracker will be available in the referenceQueue
                    @Suppress("UnsafeCast")
                    (referenceQueue.remove() as TrackedReference).apply {
                        // Clear the tracker
                        trackers.remove(uuid)
                        // Execute the operation when the associated referent object is reclaimed by the garbage
                        // collector
                        operation()
                    }
                } catch (ignored: InterruptedException) {
                }
            }
        }
    }
}
