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

### Build the code 

```
mvn package
```

Afterwards can run the `Interpretation` class in the IDEA using:

1. Enable and add an environment variable with `--add-opens=java.base/java.nio=ALL-UNNAMED` 
2. Adding provided dependencies to the Classpath
3. Changing the absolute location of the Jar file in the code

Look in `/tmp` for the results.

TODO... explain how to run it on K8s
```
cat $FILE | kubectl exec -i -n uat spark-shell-gateway-fd744fcd4-z9qm4 "--" sh -c "cat > /tmp/${FILE}"
```