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

import com.google.protobuf.Empty
import com.google.protobuf.util.Timestamps
import com.google.type.LatLng
import io.cloudstate.javasupport.eventsourced.CommandContext
import io.cloudstate.kotlinsupport.annotations.EntityId
import io.cloudstate.kotlinsupport.annotations.eventsourced.*
import org.apache.lucene.util.SloppyMath
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@EventSourcedEntity
class ActivityEntity(@EntityId private val userId: String) {

    private val MAX_VELOCITY = 20.0

    private val transactions: MutableList<Transaction> = mutableListOf()

    private fun LatLng.url(): String {
        return "https://www.google.com/maps/@$latitude,$longitude,15z"
    }

    @Snapshot
    fun snapshot(): Transactions = Transactions.newBuilder().addAllTransactions(transactions).build()

    @SnapshotHandler
    fun handleSnapshot(snapshot: Transactions) {
        transactions.clear()
        transactions.addAll(snapshot.transactionsList)
    }

    @CommandHandler
    fun addTransaction(userTransaction: UserTransaction, ctx: CommandContext): Empty {
        val maybePreviousTransaction: Optional<Transaction> = transactions.stream()
                .max { a, b -> Timestamps.compare(a.timestamp, b.timestamp) }

        val maybeDistanceInMeters = maybePreviousTransaction.map { previousTransaction ->
            val previous: LatLng = previousTransaction.location
            val current: LatLng = userTransaction.transaction.location
            SloppyMath.haversinMeters(previous.latitude, previous.longitude, current.latitude, current.longitude)
        }

        val maybeTimeBetween = maybePreviousTransaction.map { previousTransaction ->
            Timestamps.between(previousTransaction.timestamp, userTransaction.transaction.timestamp)
        }

        // m/s
        val maybeVelocity = maybeDistanceInMeters.flatMap { distance ->
            maybeTimeBetween.map { time -> distance / time.seconds }
        }

        // yeah, this is a terrible fraud detection method
        // but we have great data here for ML
        if (maybeVelocity.orElse(0.0) > MAX_VELOCITY) {
            val dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.US).withZone(ZoneId.systemDefault())
            val instant = Instant.ofEpochSecond(userTransaction.transaction.timestamp.seconds)
            println("\nPOSSIBLE FRAUD!!!")
            println("userId: $userId")
            println("Merchant: ${userTransaction.transaction.description}")
            println("Amount: $${userTransaction.transaction.amount.units}.00")
            println("On: ${dtf.format(instant)}")
            println("Map: ${userTransaction.transaction.location.url()}")
        }

        ctx.emit(TransactionAdded.newBuilder().setTransaction(userTransaction.transaction).build())

        return Empty.getDefaultInstance()
    }

    @EventHandler
    fun transactionAdded(transactionAdded: TransactionAdded) {
        transactions.add(transactionAdded.transaction)
    }

    @CommandHandler
    fun getTransactions(): Transactions {
        return snapshot()
    }

}
