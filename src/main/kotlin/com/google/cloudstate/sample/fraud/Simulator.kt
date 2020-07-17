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

import com.devskiller.jfairy.Fairy
import com.google.protobuf.util.Timestamps
import com.google.type.LatLng
import com.google.type.Money
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.random.Random

fun main(args: Array<String>) = runBlocking {
    val target = args.firstOrNull() ?: "localhost:9000"
    val channelBuilder = ManagedChannelBuilder.forTarget(target)

    val channel = if (target.endsWith(":443"))
        channelBuilder.useTransportSecurity().build()
    else
        channelBuilder.usePlaintext().build()

    val stub = ActivityServiceGrpcKt.ActivityServiceCoroutineStub(channel)

    // todo: give each user their own time
    var time = Instant.parse("2007-12-03T10:15:30.00Z").toEpochMilli()

    val userLocations = mutableMapOf(
            UUID.randomUUID() to LatLng.newBuilder().setLatitude(39.756138).setLongitude(-104.927200).build(),
            UUID.randomUUID() to LatLng.newBuilder().setLatitude(37.7577627).setLongitude(-122.4726193).build(),
            UUID.randomUUID() to LatLng.newBuilder().setLatitude(41.2919667).setLongitude(-96.1512877).build(),
            UUID.randomUUID() to LatLng.newBuilder().setLatitude(33.6056219).setLongitude(-112.2651324).build(),
            UUID.randomUUID() to LatLng.newBuilder().setLatitude(33.7678358).setLongitude(-84.4906432).build()
    )

    val minTimestampIncrement = Duration.ofMinutes(15).toMillis()
    val maxTimestampIncrement = Duration.ofDays(1).toMillis()
    val minAmount = 1
    val maxAmount = 300
    val minLatLngIncrement = -1.0
    val maxLatLngIncrement = 1.0
    val fairy = Fairy.create()
    val randomLatLngIncrement = { Random.nextDouble(minLatLngIncrement, maxLatLngIncrement) }
    val randomTimeIncrement = { Random.nextLong(minTimestampIncrement, maxTimestampIncrement) }
    val randomAmount = { Random.nextInt(minAmount, maxAmount) }
    val randomCompany = { fairy.company().name }

    while (true) {
        time += randomTimeIncrement()
        val timestamp = Timestamps.fromMillis(time)

        val amount = randomAmount()

        val userId = userLocations.keys.shuffled().first()

        val currentLocation = userLocations[userId]!!

        val newLocation = LatLng.newBuilder()
                .setLatitude(currentLocation.latitude + randomLatLngIncrement())
                .setLongitude(currentLocation.longitude + randomLatLngIncrement())
                .build()

        userLocations[userId] = newLocation

        val transaction = Transaction.newBuilder()
                .setDescription(randomCompany())
                .setTimestamp(timestamp)
                .setLocation(newLocation)
                .setAmount(Money.newBuilder().setUnits(amount.toLong()).build())
                .build()

        val userTransaction = UserTransaction.newBuilder().setUserId(userId.toString()).setTransaction(transaction).build()
        println(userTransaction)
        stub.addTransaction(userTransaction)

        /*
        UserTransactions transactions = stub.getTransactions(GetUserTransactions.newBuilder().setUserId(userId.toString()).build());
        println(transactions);
         */

        delay(100)
    }
}
