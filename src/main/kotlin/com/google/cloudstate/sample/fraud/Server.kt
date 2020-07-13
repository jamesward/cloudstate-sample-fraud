package com.google.cloudstate.sample.fraud

import io.cloudstate.kotlinsupport.cloudstate

fun main() {
    cloudstate {
        crdt {
            entityService = FraudEntity::class
            descriptor = Fraud.getDescriptor().findServiceByName("Activity")
        }
    }.start().toCompletableFuture().get()
}
