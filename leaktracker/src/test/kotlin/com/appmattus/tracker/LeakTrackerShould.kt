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

@file:Suppress("ExplicitGarbageCollectionCall")

package com.appmattus.tracker

import org.hamcrest.core.StringStartsWith
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class LeakTrackerShould {
    @Suppress("MemberVisibilityCanPrivate", "DEPRECATION")
    @get:Rule
    var thrown: ExpectedException = ExpectedException.none()

    private lateinit var exceptionLatch: CountDownLatch
    private lateinit var unsubscribeLatch: CountDownLatch
    private lateinit var tracker: LeakTracker
    private lateinit var unsubscribe: () -> Unit

    @Suppress("UNCHECKED_CAST")
    private fun <T> uninitialized(): T = null as T

    @Before
    fun setup() {
        exceptionLatch = CountDownLatch(1)
        unsubscribeLatch = CountDownLatch(1)

        tracker = LeakTracker {
            exceptionLatch.countDown()
        }

        unsubscribe = fun() {
            unsubscribeLatch.countDown()
        }
    }

    @Test
    fun `throw exception when unsubscribeOperation is null`() {
        // expect exception
        thrown.expect(IllegalArgumentException::class.java)
        thrown.expectMessage(StringStartsWith("Parameter specified as non-null is null"))

        // when unsubscribeOperation is null
        tracker.subscribe(uninitialized())
    }

    @Test
    fun `execute operation when unsubscriber de-referenced`() {
        // given
        @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
        var unsubscriber: Unsubscriber? = tracker.subscribe(unsubscribe)

        // when unsubscriber de-referenced
        @Suppress("UNUSED_VALUE")
        unsubscriber = null
        System.gc()

        // then subscriptionHandler is executed
        Assert.assertTrue("subscriptionHandler not executed", exceptionLatch.await(50, TimeUnit.MILLISECONDS))
        Assert.assertTrue(unsubscribeLatch.await(50, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `execute operation when unsubscriber never referenced`() {
        // given
        tracker.subscribe(unsubscribe)

        // when unsubscriber de-referenced
        System.gc()

        // then subscriptionHandler is executed
        Assert.assertTrue("subscriptionHandler not executed", exceptionLatch.await(50, TimeUnit.MILLISECONDS))
        Assert.assertTrue(unsubscribeLatch.await(50, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `not execute operation when unsubscribe() called and unsubscriber is de-referenced`() {
        // given
        var unsubscriber: Unsubscriber? = tracker.subscribe(unsubscribe)

        // when tracker is removed and unsubscriber de-referenced
        unsubscriber?.unsubscribe()
        @Suppress("UNUSED_VALUE")
        unsubscriber = null
        System.gc()

        // then subscriptionHandler is not executed
        Assert.assertFalse(exceptionLatch.await(50, TimeUnit.MILLISECONDS))
        Assert.assertTrue(unsubscribeLatch.await(50, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `not execute operation when unsubscribe() called and unsubscriber is referenced`() {
        // given
        val unsubscriber: Unsubscriber = tracker.subscribe(unsubscribe)

        // when tracker is removed
        unsubscriber.unsubscribe()
        System.gc()

        // then subscriptionHandler is not executed
        Assert.assertFalse(exceptionLatch.await(50, TimeUnit.MILLISECONDS))
        Assert.assertTrue(unsubscribeLatch.await(50, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `not execute operation when unsubscriber referenced`() {
        // given
        @Suppress("UNUSED_VARIABLE")
        val unsubscriber: Unsubscriber = tracker.subscribe(unsubscribe)

        // when unsubscriber is still referenced
        System.gc()

        // then subscriptionHandler is not executed
        Assert.assertFalse(exceptionLatch.await(50, TimeUnit.MILLISECONDS))
        Assert.assertFalse(unsubscribeLatch.await(50, TimeUnit.MILLISECONDS))

        unsubscriber.unsubscribe()
    }
}
