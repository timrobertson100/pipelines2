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
package org.gbif.pipelines.interpretation.spark.udf;

import org.gbif.pipelines.clients.taxonomy.MatchResponse;
import org.gbif.pipelines.clients.taxonomy.Taxonomy;
import org.gbif.pipelines.interpretation.spark.Config;

import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF13;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TaxonomyUDF
    implements UDF13<
        Integer,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        String,
        Row> {
  private transient Taxonomy taxonomy;
  private final Object lock = new Object[0];
  private final String baseUrl;
  private final int poolSize;

  public static void register(SparkSession spark, String name, Config conf) {
    spark
        .udf()
        .register(
            name,
            new TaxonomyUDF(conf.getTaxonomy().getBaseUrl(), conf.getTaxonomy().getPoolSize()),
            DataTypes.createStructType(
                new StructField[] {
                  DataTypes.createStructField("taxonId", DataTypes.StringType, true),
                  DataTypes.createStructField("scientificName", DataTypes.StringType, true)
                }));
  }

  private Taxonomy taxonomy() {
    if (taxonomy == null)
      synchronized (lock) {
        taxonomy = new Taxonomy(baseUrl, poolSize);
      }
    return taxonomy;
  }

  @Override
  public Row call(
      Integer usageKey,
      String kingdom,
      String phylum,
      String clazz,
      String order,
      String family,
      String genus,
      String scientificName,
      String genericName,
      String specificEpithet,
      String infraspecificEpithet,
      String scientificNameAuthorship,
      String rank) {
    MatchResponse.Usage usage =
        taxonomy()
            .match(
                usageKey,
                kingdom,
                phylum,
                clazz,
                order,
                family,
                genus,
                scientificName,
                genericName,
                specificEpithet,
                infraspecificEpithet,
                scientificNameAuthorship,
                rank,
                false,
                false);

    return usage != null ? RowFactory.create(usage.getKey(), usage.getName()) : null;
  }
}
