package com.google.cloudstate.sample.fraud;

import com.devskiller.jfairy.Fairy;
import com.google.cloudstate.samples.fraud.*;
import com.google.cloudstate.samples.fraud.ActivityGrpc.ActivityBlockingStub;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import com.google.type.LatLng;
import com.google.type.Money;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

public class Simulator {

    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget("localhost:9000").usePlaintext().build();

        ActivityBlockingStub stub = ActivityGrpc.newBlockingStub(channel);

        UUID userId = UUID.randomUUID();

        long time = Instant.parse("2007-12-03T10:15:30.00Z").toEpochMilli();
        LatLng location = LatLng.newBuilder().setLatitude(39.756138).setLongitude(-104.927200).build();

        long minTimestampIncrement = Duration.ofMinutes(15).toMillis();
        long maxTimestampIncrement = Duration.ofDays(1).toMillis();

        int minAmount = 1;
        int maxAmount = 300;

        double minLatLngIncrement = 0.00000001;
        double maxLatLngIncrement = 1;

        Fairy fairy = Fairy.create();

        Supplier<Double> randomLatLngIncrement = () -> ThreadLocalRandom.current().nextDouble(minLatLngIncrement, maxLatLngIncrement);
        Supplier<Long> randomTimeIncrement = () -> ThreadLocalRandom.current().nextLong(minTimestampIncrement, maxTimestampIncrement);
        Supplier<Integer> randomAmount = () -> ThreadLocalRandom.current().nextInt(minAmount, maxAmount);
        Supplier<String> randomCompany = () -> fairy.company().getName();

        while (true) {
            time += randomTimeIncrement.get();
            Timestamp timestamp = Timestamps.fromMillis(time);

            int amount = randomAmount.get();

            location = LatLng.newBuilder()
                    .setLatitude(location.getLatitude() + randomLatLngIncrement.get())
                    .setLongitude(location.getLongitude() + randomLatLngIncrement.get())
                    .build();

            Transaction transaction = Transaction.newBuilder()
                    .setDescription(randomCompany.get())
                    .setTimestamp(timestamp)
                    .setLocation(location)
                    .setAmount(Money.newBuilder().setUnits(amount).build())
                    .build();

            System.out.println(transaction);

            UserTransaction request = UserTransaction.newBuilder().setUserId(userId.toString()).setTransaction(transaction).build();

            stub.addTransaction(request);

            /*
            UserTransactions transactions = stub.getTransactions(GetUserTransactions.newBuilder().setUserId(userId.toString()).build());

            System.out.println(transactions);
             */

            Thread.sleep(1000);
        }
    }

}
