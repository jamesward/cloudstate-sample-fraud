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
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Supplier

fun main(args: Array<String>) = runBlocking {
    val target = args.firstOrNull() ?: "localhost:9000"
    val channelBuilder = ManagedChannelBuilder.forTarget(target)

    val channel = if (target.endsWith(":443"))
        channelBuilder.useTransportSecurity().build()
    else
        channelBuilder.usePlaintext().build()

    val stub = ActivityGrpcKt.ActivityCoroutineStub(channel)
    val userId = UUID.randomUUID()
    var time = Instant.parse("2007-12-03T10:15:30.00Z").toEpochMilli()
    var location = LatLng.newBuilder().setLatitude(39.756138).setLongitude(-104.927200).build()
    val minTimestampIncrement = Duration.ofMinutes(15).toMillis()
    val maxTimestampIncrement = Duration.ofDays(1).toMillis()
    val minAmount = 1
    val maxAmount = 300
    val minLatLngIncrement = 0.00000001
    val maxLatLngIncrement = 1.0
    val fairy = Fairy.create()
    val randomLatLngIncrement = Supplier { ThreadLocalRandom.current().nextDouble(minLatLngIncrement, maxLatLngIncrement) }
    val randomTimeIncrement = Supplier { ThreadLocalRandom.current().nextLong(minTimestampIncrement, maxTimestampIncrement) }
    val randomAmount = Supplier { ThreadLocalRandom.current().nextInt(minAmount, maxAmount) }
    val randomCompany = Supplier { fairy.company().name }

    while (true) {
        time += randomTimeIncrement.get()
        val timestamp = Timestamps.fromMillis(time)

        val amount = randomAmount.get()

        location = LatLng.newBuilder()
                .setLatitude(location.latitude + randomLatLngIncrement.get())
                .setLongitude(location.longitude + randomLatLngIncrement.get())
                .build()

        val transaction = Transaction.newBuilder()
                .setDescription(randomCompany.get())
                .setTimestamp(timestamp)
                .setLocation(location)
                .setAmount(Money.newBuilder().setUnits(amount.toLong()).build())
                .build()

        println(transaction)

        val userTransaction = UserTransaction.newBuilder().setUserId(userId.toString()).setTransaction(transaction).build()
        stub.addTransaction(userTransaction)

        /*
        UserTransactions transactions = stub.getTransactions(GetUserTransactions.newBuilder().setUserId(userId.toString()).build());
        println(transactions);
         */

        delay(1000)
    }
}
