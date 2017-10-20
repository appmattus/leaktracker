# leaktracker [![Build Status](https://travis-ci.org/appmattus/leaktracker.svg?branch=master)](https://travis-ci.org/appmattus/leaktracker) [![Coverage Status](https://coveralls.io/repos/github/appmattus/leaktracker/badge.svg?branch=master)](https://coveralls.io/github/appmattus/leaktracker?branch=master)

A memory leak tracking library for Android and Java.

If you are developing an API where your clients must bind and unbind with their observers, then wouldn’t it be better if
you could reduce the chance of misuse?

LeakTracker helps detect when your observers aren't unbound properly.

## Getting started

Somewhere in your code store a reference to the tracker.
[Dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) such as
[Dagger](https://google.github.io/dagger/) is recommended to inject this instance where it is needed.

```kotlin
val tracker = LeakTracker { exception ->
    // Log the exception as you wish
    Log.e(LOG_TAG, "Subscription leaked", exception)
}
```

Then in your APIs addObserver/bind/subscribe method use the `tracker.subscribe` function to return an Unsubscriber.

```kotlin
@CheckResult
fun addObserver(observer: Observer): Unsubscriber {
    // save your observer here

    return tracker.subscribe {
        // remove your observer here
    }
}
```

LeakTracker will automatically call your exception handler when an `Unsubscriber` is garbage collected without being
called.

## Download [![Download](https://api.bintray.com/packages/appmattus/maven/leaktracker/images/download.svg) ](https://bintray.com/appmattus/maven/leaktracker/_latestVersion)

```groovy
dependencies {
    compile 'com.appmattus:leaktracker:<latest-version>'
}
```

## Contributing
Please fork this repository and contribute back using [pull requests](https://github.com/appmattus/leaktracker/pulls).

All contributions, large or small, major features, bug fixes, additional language translations, unit/integration tests are welcomed.

## License [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Copyright 2017 Appmattus Limited

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
