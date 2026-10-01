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

/**
 * Notifies you when the referent object is garbage collected.
 *
 * Garbage collection can only be observed on the JVM, JS and Kotlin/Native targets. On wasmJs and wasmWasi, where no
 * weak reference mechanism is available, subscriptions are never reported as leaked.
 */
public expect class LeakTracker(exceptionHandler: (Exception) -> Unit) {
    /**
     * Tracks the Unsubscriber returned, calling the provided exception handler if unsubscribe() is not called before
     * the Unsubscriber is garbage collected
     *
     * @param unsubscribeOperation  the operation to execute when unsubscribe() is called
     */
    public fun subscribe(unsubscribeOperation: () -> Unit): Unsubscriber
}
