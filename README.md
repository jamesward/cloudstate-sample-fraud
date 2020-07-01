# Cloudstate Sample Fraud Detection


```
./mvnw compile exec:java -Dexec.mainClass="com.google.cloudstate.sample.fraud.Server"
```

```
docker run -it --rm --network="host" cloudstateio/cloudstate-proxy-dev-mode -Dcloudstate.proxy.user-function-port=8080 -Dcloudstate.proxy.passivation-timeout=60m
```

```
./mvnw compile exec:java -Dexec.mainClass="com.google.cloudstate.sample.fraud.Simulator"
```
