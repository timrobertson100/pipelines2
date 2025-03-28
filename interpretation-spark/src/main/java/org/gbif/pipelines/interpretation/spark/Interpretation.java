/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.pipelines.interpretation.spark;

import org.apache.spark.api.java.function.MapFunction;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.interpretation.spark.udf.DictionaryUDF;
import org.gbif.pipelines.interpretation.spark.udf.TaxonomyUDF;

import java.io.Serializable;

import org.apache.spark.sql.*;

import static org.gbif.pipelines.interpretation.spark.InterpretationSQL.*;

public class Interpretation implements Serializable {
  private static final Config CONF = new Config(); // TODO: a proper conf design

  public static void main(String[] args) {
    String input = "/Users/tsj442/dev/data/svampeatlas.verbatim.avro";
    String output = "/tmp/svampeatlas-interpreted";

    // String input = "hdfs:///data/ingest/50c9509d-22c7-4a22-a47d-8c48425ef4a7/229/verbatim.avro";
    // String input = "hdfs:///data/ingest/84d26682-f762-11e1-a439-00145eb45e9a/243/verbatim.avro";

    SparkSession spark = SparkSession.builder().remote("sc://localhost").getOrCreate();
    // SparkSession spark = SparkSession.builder().getOrCreate();

    spark.addArtifact(
        "./interpretation-spark/target/interpretation-spark-2.0.0-SNAPSHOT-3.5.4.jar");

    Dataset<ExtendedRecord> records =
        spark.read().format("avro").load(input).as(Encoders.bean(ExtendedRecord.class));
    // records.createOrReplaceTempView("source");

    /*
    MapFunction<ExtendedRecord, BasicRecord> f =
        er ->
            new BasicRecord(
                er.getId(), er.getCoreTerms().get("http://rs.tdwg.org/dwc/terms/scientificName"));
*/

    //Dataset<BasicRecord> basic = records.map(f, Encoders.bean(BasicRecord.class));

    //basic.write().mode("overwrite").csv(output);

    // SparkSession spark = SparkSession.builder().getOrCreate();

    // read the verbatim avro input
    spark.read().format("avro").load(input).createOrReplaceTempView("verbatim");

    // create a view on the key value verbatim data to simplify SQL and improve performance
    spark.sql(SOURCE_VIEW);

    // generate dictionaries for all controlled fields, avoid cache use
    DictionaryUDF.register(spark, "dictionaryLookup", CONF);
    for (String dictionary : DICTIONARIES) {
      spark.sql(dictionary);
    }

    // generate a table for the taxonomy, avoid cache use
    TaxonomyUDF.register(spark, "nameMatch", CONF);
    spark.sql(TAXONOMY);

    // parse fields using functions and dictionary lookups
    spark.sql(PARSE_SOURCE);

    // TODO expand with fields that are not interpreted

    spark.sql("SELECT * FROM parsed").write().mode("overwrite").csv(output);

    spark.close();
  }
}
