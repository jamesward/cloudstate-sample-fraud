package com.google.cloudstate.sample.fraud

import com.devskiller.jfairy.Fairy
import com.google.protobuf.util.Timestamps
import com.google.type.LatLng
import com.google.type.Money
import io.grpc.ManagedChannelBuilder
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.function.Supplier

fun main() {
    val channel = ManagedChannelBuilder.forTarget("localhost:9000").usePlaintext().build()
    val stub = ActivityGrpc.newBlockingStub(channel)
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

        val request = UserTransaction.newBuilder().setUserId(userId.toString()).setTransaction(transaction).build()
        stub.addTransaction(request)

        /*
        UserTransactions transactions = stub.getTransactions(GetUserTransactions.newBuilder().setUserId(userId.toString()).build());
        println(transactions);
         */

        Thread.sleep(1000)
    }
}
