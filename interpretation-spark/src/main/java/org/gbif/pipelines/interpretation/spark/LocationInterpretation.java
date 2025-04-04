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

import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.models.LocationRecord;
import org.gbif.pipelines.models.MetadataRecord;
import org.gbif.pipelines.transform.LocationTransform;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

public class LocationInterpretation {

  /** Transforms the source records into the taxon records using the lookup service. */
  public static Dataset<LocationRecord> locationTransform(
      Config config, SparkSession spark, Dataset<ExtendedRecord> source) {
    LocationTransform locationTransform =
        LocationTransform.builder().geocodeApiUrl(config.getSpeciesMatchAPI()).build();

    return source.map(
        (MapFunction<ExtendedRecord, LocationRecord>)
            er ->
                locationTransform
                    .convert(er, MetadataRecord.newBuilder().build()) // TODO
                    .get(),
        Encoders.bean(LocationRecord.class));
  }
}
