package com.google.cloudstate.sample.fraud

import com.google.protobuf.Empty
import com.google.protobuf.util.Timestamps
import com.google.type.LatLng
import io.cloudstate.javasupport.crdt.CommandContext
import io.cloudstate.javasupport.crdt.GSet
import io.cloudstate.kotlinsupport.annotations.crdt.CommandHandler
import io.cloudstate.kotlinsupport.annotations.crdt.CrdtEntity
import org.apache.lucene.util.SloppyMath
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@CrdtEntity
class FraudEntity(private val transactions: GSet<UserTransaction>) {

    private val MAX_VELOCITY = 20.0

    @CommandHandler
    fun addTransaction(transaction: UserTransaction, ctx: CommandContext?): Empty {
        val maybePreviousTransaction: Optional<Transaction> = transactions.stream()
                .map(UserTransaction::getTransaction)
                .max { a, b -> Timestamps.compare(a.timestamp, b.timestamp) }

        val maybeDistanceInMeters = maybePreviousTransaction.map { previousTransaction ->
            val previous: LatLng = previousTransaction.location
            val current: LatLng = transaction.transaction.location
            SloppyMath.haversinMeters(previous.latitude, previous.longitude, current.latitude, current.longitude)
        }

        val maybeTimeBetween = maybePreviousTransaction.map { previousTransaction ->
            Timestamps.between(previousTransaction.timestamp, transaction.transaction.timestamp)
        }

        // m/s
        val maybeVelocity = maybeDistanceInMeters.flatMap { distance: Double ->
            maybeTimeBetween.map { time -> distance / time.seconds }
        }

        // yeah, this is a terrible fraud detection method
        // but we have great data here for ML
        if (maybeVelocity.orElse(0.0) > MAX_VELOCITY) {
            val dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.US).withZone(ZoneId.systemDefault())
            val instant = Instant.ofEpochSecond(transaction.transaction.timestamp.seconds)
            println("\nPOSSIBLE FRAUD!!!")
            println("Merchant: " + transaction.transaction.description)
            println("Amount: $" + transaction.transaction.amount.units.toString() + ".00")
            println("On: " + dtf.format(instant))
            println("Map: https://www.google.com/maps/@" + transaction.transaction.location.latitude.toString() + "," + transaction.transaction.location.longitude.toString() + ",15z")
        }

        transactions.add(transaction)
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun getTransactions(): UserTransactions {
        return UserTransactions.newBuilder().addAllTransactions(transactions).build()
    }

}
