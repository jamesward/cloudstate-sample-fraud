package com.google.cloudstate.sample.fraud

import com.google.cloudstate.sample.fraud.Transaction
import com.google.cloudstate.sample.fraud.UserTransaction
import com.google.cloudstate.sample.fraud.UserTransactions
import com.google.protobuf.Duration
import com.google.protobuf.Empty
import com.google.protobuf.util.Timestamps
import com.google.type.LatLng
import io.cloudstate.javasupport.crdt.CommandContext
import io.cloudstate.javasupport.crdt.CommandHandler
import io.cloudstate.javasupport.crdt.CrdtEntity
import io.cloudstate.javasupport.crdt.GSet
import org.apache.lucene.util.SloppyMath
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@CrdtEntity
class FraudEntity(val transactions: GSet<UserTransaction>) {

    private val MAX_VELOCITY = 20.0

    @CommandHandler
    fun addTransaction(transaction: UserTransaction, ctx: CommandContext?): Empty {
        val maybePreviousTransaction: Optional<Transaction> = transactions.stream()
                .max { a: UserTransaction, b: UserTransaction -> Timestamps.compare(a.getTransaction().getTimestamp(), b.getTransaction().getTimestamp()) }
                .map<Transaction>(UserTransaction::getTransaction)
        val maybeDistanceInMeters = maybePreviousTransaction.map { previousTransaction: Transaction ->
            val previous: LatLng = previousTransaction.getLocation()
            val current: LatLng = transaction.getTransaction().getLocation()
            SloppyMath.haversinMeters(previous.latitude, previous.longitude, current.latitude, current.longitude)
        }
        val maybeTimeBetween = maybePreviousTransaction.map { previousTransaction: Transaction -> Timestamps.between(previousTransaction.getTimestamp(), transaction.getTransaction().getTimestamp()) }

        // m/s
        val maybeVelocity = maybeDistanceInMeters.flatMap { distance: Double ->
            maybeTimeBetween.map { time: Duration -> distance / time.seconds }
        }

        // yeah, this is a terrible fraud detection method
        // but we have great data here for ML
        if (maybeVelocity.orElse(0.0) > MAX_VELOCITY) {
            val dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.US).withZone(ZoneId.systemDefault())
            val instant = Instant.ofEpochSecond(transaction.getTransaction().getTimestamp().getSeconds())
            println("\nPOSSIBLE FRAUD!!!")
            System.out.println("Merchant: " + transaction.getTransaction().getDescription())
            System.out.println("Amount: $" + transaction.getTransaction().getAmount().getUnits().toString() + ".00")
            println("On: " + dtf.format(instant))
            System.out.println("Map: https://www.google.com/maps/@" + transaction.getTransaction().getLocation().getLatitude().toString() + "," + transaction.getTransaction().getLocation().getLongitude().toString() + ",15z")
        }
        transactions.add(transaction)
        return Empty.getDefaultInstance()
    }

    @CommandHandler
    fun getTransactions(): UserTransactions {
        return UserTransactions.newBuilder().addAllTransactions(transactions).build()
    }

}
