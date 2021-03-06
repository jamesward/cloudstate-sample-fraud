// Copyright 2020 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloudstate.sample.fraud

import io.cloudstate.kotlinsupport.cloudstate

fun main() {
    cloudstate {
        config {
            port = 8080
        }

        eventsourced {
            entityService = ActivityEntity::class
            descriptor = Fraud.getDescriptor().findServiceByName("ActivityService")
            additionalDescriptors = listOf(Fraud.getDescriptor())
            snapshotEvery = 1
            persistenceId = "activity"
        }
    }.start().toCompletableFuture().get()
}
