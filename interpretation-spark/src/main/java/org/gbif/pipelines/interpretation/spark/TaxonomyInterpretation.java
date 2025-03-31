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
import org.gbif.kvs.species.Identification;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.models.taxonomy.TaxonRecord;
import org.gbif.pipelines.transform.TaxonomyTransform;

import java.util.Optional;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.gbif.pipelines.utils.ModelUtils.extractValue;

/**
 * Utility class to create an efficient transformation of the source records into the TaxonRecord.
 * This extracts the unique identifications from the source records, noting they may lie in
 * extensions, and creates a dictionary lookup mapping to the taxon record, and then joins the
 * dictionary back to the source record. This approach means that only a single lookup is made per
 * distinct identification.
 */
public class TaxonomyInterpretation {

  /** Transforms the source records into the taxon records using the lookup service. */
  public static Dataset<TaxonRecord> taxonomyTransform(
      Config config, SparkSession spark, Dataset<ExtendedRecord> records) {
    TaxonomyTransform taxonomyTransform =
        TaxonomyTransform.builder().speciesApiUrl(config.getSpeciesMatchAPI()).build();

    // extract the identification, noting that it might be in an extension
    Dataset<RecordWithIdentification> recordWithIdentification =
        records.map(
            (MapFunction<ExtendedRecord, RecordWithIdentification>)
                er -> {
                  Identification identification = taxonomyTransform.extractIdentification(er);
                  return RecordWithIdentification.builder()
                      .id(er.getId())
                      .coreId(er.getCoreId())
                      .parentId(extractValue(er, DwcTerm.parentEventID))
                      .identificationHash(hash(identification))
                      .identification(identification)
                      .build();
                },
            Encoders.bean(RecordWithIdentification.class));
    recordWithIdentification.createOrReplaceTempView("record_with_identification");

    // distinct the identifications to lookup
    Dataset<Identification> distinctIdentifications =
        spark
            .sql("SELECT DISTINCT identification.* FROM record_with_identification")
            .repartition(config.getSpeciesMatchParallelism())
            .as(Encoders.bean(Identification.class));

    // lookup the distinct identifications, and create a dictionary of the results
    Dataset<KeyedTaxonRecord> keyedTaxa =
        distinctIdentifications.map(
            (MapFunction<Identification, KeyedTaxonRecord>)
                identification -> {
                  Optional<TaxonRecord> matched = taxonomyTransform.lookup(identification);
                  if (matched.isPresent())
                    return KeyedTaxonRecord.builder()
                        .key(hash(identification))
                        .taxonRecord(matched.get())
                        .build();
                  else return null;
                },
            Encoders.bean(KeyedTaxonRecord.class));
    keyedTaxa.createOrReplaceTempView("key_taxa");

    // join the dictionary back to the source records
    Dataset<RecordWithTaxonRecord> expanded =
        spark
            .sql(
                "SELECT id, coreId, parentId, taxonRecord "
                    + "FROM record_with_identification r "
                    + "  LEFT JOIN key_taxa tr ON r.identificationHash = tr.key")
            .as(Encoders.bean(RecordWithTaxonRecord.class));

    return expanded.map(
        (MapFunction<RecordWithTaxonRecord, TaxonRecord>)
            r -> {
              TaxonRecord taxonRecord = r.getTaxonRecord();
              taxonRecord.setId(r.getId());
              taxonRecord.setId(r.getCoreId());
              taxonRecord.setId(r.getParentId());
              return taxonRecord;
            },
        Encoders.bean(TaxonRecord.class));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecordWithIdentification {
    private String id;
    private String coreId;
    private String parentId;
    private String identificationHash;
    private Identification identification;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KeyedTaxonRecord {
    private String key;
    private TaxonRecord taxonRecord;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecordWithTaxonRecord {
    private String id;
    private String coreId;
    private String parentId;
    private TaxonRecord taxonRecord;
  }

  /** Uniquely key a identification */
  public static String hash(Identification i) {
    return String.join(
        "|",
        i.getScientificName(),
        i.getTaxonConceptID(),
        i.getTaxonID(),
        i.getKingdom(),
        i.getPhylum(),
        i.getClazz(),
        i.getOrder(),
        i.getFamily(),
        i.getGenus(),
        i.getScientificName(),
        i.getGenericName(),
        i.getSpecificEpithet(),
        i.getInfraspecificEpithet(),
        i.getScientificNameAuthorship(),
        i.getRank(),
        i.getRank());
  }
}
