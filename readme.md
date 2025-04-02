## POC GBIF Spark connect Pipelines

This reads a verbatim avro file, and produces the Parquet and JSON output suitable for 
the SQL and ElasticSearch engines.

This is designed to:

1. Be a lightweight codebase with minimal dependencies
2. Build to a small footprint and build quickly 
3. Minimise infrastructure dependencies 
4. Avoid complex caching by using efficient processing (e.g. distinct values) 

Spark running on a laptop processes the 1M Svampeatlas dataset in around 10 secs.

### Install spark locally

Download and start a Spark cluster and set a few things (this may vary with environments):

```
wget https://dlcdn.apache.org/spark/spark-3.5.5/spark-3.5.5-bin-hadoop3.tgz        
tar -xvf spark-3.5.5-bin-hadoop3.tgz
cd spark-3.5.5-bin-hadoop3
export SPARK_LOCAL_IP="127.0.0.1"
export SPARK_DAEMON_MEMORY="4G"
./sbin/start-connect-server.sh --packages org.apache.spark:spark-connect_2.12:3.5.5,org.apache.spark:spark-avro_2.12:3.5.5
```

Make sure you are on Java 17.

### Build the code 

```
mvn package
```

Afterwards can run the `Interpretation` class in the IDEA using:

1. Adding provided dependencies to the Classpath
2. Changing the absolute location of the Jar file in the code

Look in `/tmp` for the results.

TODO... explain how to run it on K8s
```
cat $FILE | kubectl exec -i -n uat spark-shell-gateway-fd744fcd4-z9qm4 "--" sh -c "cat > /tmp/${FILE}"

./bin/spark-submit --master k8s://https://uatcontrolplane1-vh.gbif-uat.org --deploy-mode client --executor-memory 12G \
    --driver-memory 4G --num-executors 20 --executor-cores 6 \
    --packages org.apache.spark:spark-avro_2.12:3.5.1 \
    --conf spark.kubernetes.authenticate.serviceAccountName="uat-spark-client" \
    --conf spark.kubernetes.namespace="uat" \
    --conf spark.kubernetes.container.image="stackable-docker.gbif.org/stackable/spark-k8s:3.5.1-stackable24.3.0"\
    --conf spark.driver.host=spark-shell-gateway \
    --conf spark.driver.bindAddress=0.0.0.0 \
    --conf spark.kubernetes.driver.label.queue=root.uat.default \
    --conf spark.kubernetes.executor.label.queue=root.uat.default \
    --conf spark.kubernetes.submit.label.queue=root.uat.default \
    --conf spark.kubernetes.scheduler.name=yunikorn \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.mount.path=/data/spark/ \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.mount.readOnly=false \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.sizeLimit=5Gi \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.storageClass=spark-local-path \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.claimName=OnDemand \
    --conf spark.driver.port=7078 \
    --conf spark.blockManager.port=7089 \
    --class org.gbif.pipelines.interpretation.spark.Interpretation  \
    /tmp/interpretation-spark-2.0.0-SNAPSHOT-3.5.5.jar \
    /tmp/conf-uat.yaml
```

Or

```
kubectl config use-context production     
FILE=conf-prod.yaml 
cat $FILE | kubectl exec -i -n production spark-shell-gateway-569d7c5894-wfrbt "--" sh -c "cat > /tmp/${FILE}" 

./bin/spark-submit --master k8s://https://prodgateway-vh.gbif.org --deploy-mode client --executor-memory 16G \
    --driver-memory 4G --num-executors 40 --executor-cores 5 \
    --packages org.apache.spark:spark-avro_2.12:3.5.1 \
    --conf spark.kubernetes.authenticate.serviceAccountName="spark-shell-gateway" \
    --conf spark.kubernetes.namespace="production" \
    --conf spark.kubernetes.container.image="stackable-docker.gbif.org/stackable/spark-k8s:3.5.1-stackable24.3.0"\
    --conf spark.driver.host=spark-shell-gateway \
    --conf spark.driver.bindAddress=0.0.0.0 \
    --conf spark.kubernetes.driver.label.queue=root.uat2.default \
    --conf spark.kubernetes.executor.label.queue=root.uat2.default \
    --conf spark.kubernetes.submit.label.queue=root.uat2.default \
    --conf spark.kubernetes.scheduler.name=yunikorn \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.mount.path=/data/spark/ \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.mount.readOnly=false \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.sizeLimit=5Gi \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.storageClass=local-path-delete \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.claimName=OnDemand \
    --conf spark.driver.port=7078 \
    --conf spark.blockManager.port=7089 \
    --class org.gbif.pipelines.interpretation.spark.Interpretation  \
    /tmp/interpretation-spark-2.0.0-SNAPSHOT-3.5.5.jar \
    /tmp/conf-prod.yaml

./bin/spark-submit 
    --master k8s://https://prodgateway-vh.gbif.org \
    --deploy-mode client \
    --executor-memory 16G \
    --driver-memory 4G \
    --num-executors 80 \
    --executor-cores 5 \
    --packages org.apache.spark:spark-avro_2.12:3.5.1 \
    --conf spark.executor.memoryOverhead=4G \
	--conf spark.sql.shuffle.partitions=1200 \	
    --conf spark.kubernetes.authenticate.serviceAccountName="spark-shell-gateway" \
    --conf spark.kubernetes.namespace="production" \
    --conf spark.kubernetes.container.image="stackable-docker.gbif.org/stackable/spark-k8s:3.5.1-stackable24.3.0"\
    --conf spark.driver.host=spark-shell-gateway \
    --conf spark.driver.bindAddress=0.0.0.0 \
    --conf spark.kubernetes.driver.label.queue=root.uat2.default \
    --conf spark.kubernetes.executor.label.queue=root.uat2.default \
    --conf spark.kubernetes.submit.label.queue=root.uat2.default \
    --conf spark.kubernetes.scheduler.name=yunikorn \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.mount.path=/data/spark/ \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.mount.readOnly=false \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.sizeLimit=5Gi \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.storageClass=local-path-delete \
    --conf spark.kubernetes.executor.volumes.persistentVolumeClaim.spark-local-dir-1.options.claimName=OnDemand \
    --conf spark.driver.port=7078 \
    --conf spark.blockManager.port=7089 \
    --class org.gbif.pipelines.interpretation.spark.Interpretation  \
    /tmp/interpretation-spark-2.0.0-SNAPSHOT-3.5.5.jar \
    /tmp/conf-prod.yaml

```