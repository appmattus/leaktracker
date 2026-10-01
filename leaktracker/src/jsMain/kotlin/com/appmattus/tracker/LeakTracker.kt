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

@Suppress("UnusedPrivateProperty")
private external class FinalizationRegistry(cleanup: (Int) -> Unit) {
    fun register(target: Any, heldValue: Int, unregisterToken: Any)
    fun unregister(unregisterToken: Any)
}

/**
 * Notifies you when the referent object is garbage collected
 */
public actual class LeakTracker actual constructor(private val exceptionHandler: (Exception) -> Unit) {
    private val operations = mutableMapOf<Int, () -> Unit>()
    private var nextId = 0

    private val registry = FinalizationRegistry { id ->
        operations.remove(id)?.invoke()
    }

    /**
     * Tracks the Unsubscriber returned, calling the provided exception handler if unsubscribe() is not called before
     * the Unsubscriber is garbage collected
     *
     * @param unsubscribeOperation  the operation to execute when unsubscribe() is called
     */
    public actual fun subscribe(unsubscribeOperation: () -> Unit): Unsubscriber {
        val exception = IllegalStateException("Subscription has not been un-subscribed")
        val id = nextId++

        val unsubscriber = object : Unsubscriber {
            override fun unsubscribe() {
                operations.remove(id)
                registry.unregister(this)
                unsubscribeOperation()
            }
        }

        // The held value must not reference the unsubscriber otherwise it would never be collected
        operations[id] = {
            exceptionHandler(exception)
            unsubscribeOperation()
        }
        registry.register(unsubscriber, id, unsubscriber)

        return unsubscriber
    }
}
