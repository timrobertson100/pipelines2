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

import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.transform.BasicTransform;

import java.io.IOException;
import java.io.Serializable;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.*;

public class Interpretation implements Serializable {
  public static void main(String[] args) throws IOException {
    Config config = Config.fromFirstArg(args);

    SparkSession.Builder sb = SparkSession.builder();
    if (config.getSparkRemote() != null) sb.remote("sc://localhost");
    SparkSession spark = sb.getOrCreate();
    if (config.getJarPath() != null) spark.addArtifact(config.getJarPath());

    // Read the verbatim input
    Dataset<ExtendedRecord> records =
        spark.read().format("avro").load(config.getInput()).as(Encoders.bean(ExtendedRecord.class));

    // Run the interpretations
    Dataset<BasicRecord> basic =
        records.map(newBasicTransform(config), Encoders.bean(BasicRecord.class));

    // Write the output
    basic.write().mode("overwrite").parquet(config.getOutput());

    spark.close();
  }

  private static MapFunction<ExtendedRecord, BasicRecord> newBasicTransform(Config config) {
    return er ->
        BasicTransform.builder()
            .useDynamicPropertiesInterpretation(true)
            .vocabularyApiUrl(config.getVocabularyApiUrl())
            .build()
            .convert(er)
            .get();
  }
}
