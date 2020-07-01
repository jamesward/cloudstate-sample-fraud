package com.google.cloudstate.sample.fraud;

import com.google.cloudstate.samples.fraud.Fraud;
import io.cloudstate.javasupport.CloudState;

import java.util.concurrent.ExecutionException;

public class Server {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        new CloudState()
                .registerCrdtEntity(FraudEntity.class, Fraud.getDescriptor().findServiceByName("Activity"))
                .start()
                .toCompletableFuture()
                .get();

    }

}
