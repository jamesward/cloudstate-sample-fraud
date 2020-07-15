# Cloudstate Sample Fraud Detection

## Run Locally
```
./gradlew run
```

```
docker run -it --rm --network="host" cloudstateio/cloudstate-proxy-dev-mode -Dcloudstate.proxy.user-function-port=8088 -Dcloudstate.proxy.passivation-timeout=60m
```

```
./gradlew simulator
```


## Run on GKE

```
export PROJECT_ID=YOUR_PROJECT_ID
```

```
./gradlew jib --image=gcr.io/$PROJECT_ID/cloudstate-sample-fraud
```

```
gcloud config set project $PROJECT_ID

gcloud services enable \
  cloudapis.googleapis.com \
  container.googleapis.com \
  containerregistry.googleapis.com

# Configure default zone & region
gcloud config set compute/zone us-central1-c
gcloud config set compute/region us-central1

# Create Kubernetes cluster
gcloud container clusters create cloudstate-demo \
  --addons=HorizontalPodAutoscaling,HttpLoadBalancing \
  --machine-type=n1-standard-4 \
  --cluster-version=1.16 \
  --enable-stackdriver-kubernetes --enable-ip-alias \
  --enable-autoscaling --min-nodes=5 --num-nodes=5 --max-nodes=10 \
  --enable-autorepair \
  --scopes cloud-platform

# Setup cluster-admin
kubectl create clusterrolebinding cluster-admin-binding \
  --clusterrole=cluster-admin \
  --user=$(gcloud config get-value core/account)

kubectl create namespace cloudstate

kubectl apply -n cloudstate -f https://github.com/cloudstateio/cloudstate/releases/download/v0.5.1/cloudstate-0.5.1.yaml
```

```
cat <<EOF | kubectl apply -f -
apiVersion: cloudstate.io/v1alpha1
kind: StatefulService
metadata:
  name: fraud
spec:
  containers:
  - image: gcr.io/$PROJECT_ID/cloudstate-sample-fraud
    name: fraud
EOF
```

```
export pod=$(kubectl get pods -o custom-columns=:metadata.name|grep fraud)
kubectl port-forward $pod 9000:8013
```

```
export pod=$(kubectl get pods -o custom-columns=:metadata.name|grep fraud)
kubectl logs -f $pod -c user-container
```

```
docker stop $(docker ps -q)
./gradlew simulator
```
