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

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.parsers.vocabulary.VocabularyService;
import org.gbif.pipelines.transform.BasicTransform;
import org.gbif.vocabulary.lookup.InMemoryVocabularyLookup;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.apache.spark.api.java.function.MapPartitionsFunction;
import org.apache.spark.sql.*;

public class Interpretation implements Serializable {
  public static void main(String[] args) {
    String input = "/Users/tim/dev/data/svampeatlas.verbatim.avro";
    String output = "/tmp/svampeatlas-interpreted";

    SparkSession spark = SparkSession.builder().remote("sc://localhost").getOrCreate();

    spark.addArtifact(
        "./interpretation-spark/target/interpretation-spark-2.0.0-SNAPSHOT-3.5.5.jar");

    Dataset<ExtendedRecord> records =
        spark.read().format("avro").load(input).as(Encoders.bean(ExtendedRecord.class));

    Dataset<BasicRecord> basic =
        records.mapPartitions(
            (MapPartitionsFunction<ExtendedRecord, BasicRecord>)
                er -> {
                  BasicTransform bt =
                      BasicTransform.builder()
                          .useDynamicPropertiesInterpretation(true)
                          .vocabularyService(newVocabularyService())
                          .build();

                  List<BasicRecord> out = new LinkedList<>();
                  er.forEachRemaining(r -> out.add(bt.convert(r).get()));
                  return out.iterator();
                },
            Encoders.bean(BasicRecord.class));

    basic.write().mode("overwrite").parquet(output);

    spark.close();
  }

  private static VocabularyService newVocabularyService() {
    // TODO: prefixes from
    // https://github.com/gbif/tech-docs/blob/main/en/data-processing/modules/ROOT/pages/vocabularies-interpretation.adoc
    // TODO: Some likely missing!
    return VocabularyService.builder()
        .vocabularyLookup(
            DwcTerm.lifeStage.qualifiedName(),
            InMemoryVocabularyLookup.newBuilder()
                .from("http://api.gbif.org/v1/", "LifeStage")
                .build())
        .vocabularyLookup(
            DwcTerm.degreeOfEstablishment.qualifiedName(),
            InMemoryVocabularyLookup.newBuilder()
                .from("http://api.gbif.org/v1/", "DegreeOfEstablishment")
                .build())
        .vocabularyLookup(
            DwcTerm.establishmentMeans.qualifiedName(),
            InMemoryVocabularyLookup.newBuilder()
                .from("http://api.gbif.org/v1/", "EstablishmentMeans")
                .build())
        .vocabularyLookup(
            DwcTerm.pathway.qualifiedName(),
            InMemoryVocabularyLookup.newBuilder()
                .from("http://api.gbif.org/v1/", "Pathway")
                .build())
        .vocabularyLookup(
            DwcTerm.typeStatus.qualifiedName(),
            InMemoryVocabularyLookup.newBuilder()
                .from("http://api.gbif.org/v1/", "TypeStatus")
                .build())
        .vocabularyLookup(
            DwcTerm.sex.qualifiedName(),
            InMemoryVocabularyLookup.newBuilder().from("http://api.gbif.org/v1/", "Sex").build())
        .build();
  }
}
