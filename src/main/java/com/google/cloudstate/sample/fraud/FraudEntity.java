package com.google.cloudstate.sample.fraud;

import com.google.cloudstate.samples.fraud.Transaction;
import com.google.cloudstate.samples.fraud.UserTransaction;
import com.google.cloudstate.samples.fraud.UserTransactions;
import com.google.protobuf.Duration;
import com.google.protobuf.Empty;
import com.google.protobuf.util.Timestamps;
import com.google.type.LatLng;
import io.cloudstate.javasupport.crdt.*;
import org.apache.lucene.util.SloppyMath;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.Optional;

@CrdtEntity
public class FraudEntity {

    private Double MAX_VELOCITY = 20D;

    private final GSet<UserTransaction> transactions;

    public FraudEntity(GSet<UserTransaction> transactions) {
        this.transactions = transactions;
    }

    @CommandHandler
    public Empty addTransaction(UserTransaction transaction, CommandContext ctx) {
        Optional<Transaction> maybePreviousTransaction = transactions.stream()
                .max((a, b) -> Timestamps.compare(a.getTransaction().getTimestamp(), b.getTransaction().getTimestamp()))
                .map(UserTransaction::getTransaction);

        Optional<Double> maybeDistanceInMeters = maybePreviousTransaction.map(previousTransaction -> {
            LatLng previous = previousTransaction.getLocation();
            LatLng current = transaction.getTransaction().getLocation();
            return SloppyMath.haversinMeters(previous.getLatitude(), previous.getLongitude(), current.getLatitude(), current.getLongitude());
        });

        Optional<Duration> maybeTimeBetween = maybePreviousTransaction.map(previousTransaction ->
            Timestamps.between(previousTransaction.getTimestamp(), transaction.getTransaction().getTimestamp())
        );

        // m/s
        Optional<Double> maybeVelocity = maybeDistanceInMeters.flatMap( distance ->
                maybeTimeBetween.map( time ->
                        distance / time.getSeconds()
                )
        );

        // yeah, this is a terrible fraud detection method
        // but we have great data here for ML
        if (maybeVelocity.orElse(0D) > MAX_VELOCITY) {
            DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.US).withZone(ZoneId.systemDefault());
            Instant instant = Instant.ofEpochSecond(transaction.getTransaction().getTimestamp().getSeconds());

            System.out.println("\nPOSSIBLE FRAUD!!!");
            System.out.println("Merchant: " + transaction.getTransaction().getDescription());
            System.out.println("Amount: $" + transaction.getTransaction().getAmount().getUnits() + ".00");
            System.out.println("On: " + dtf.format(instant));
            System.out.println("Map: https://www.google.com/maps/@" + transaction.getTransaction().getLocation().getLatitude() + "," + transaction.getTransaction().getLocation().getLongitude() + ",15z");
        }

        transactions.add(transaction);

        return Empty.getDefaultInstance();
    }

    @CommandHandler
    public UserTransactions getTransactions() {
        return UserTransactions.newBuilder().addAllTransactions(transactions).build();
    }

}
