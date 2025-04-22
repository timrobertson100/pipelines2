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

import org.gbif.dwc.terms.DcTerm;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.models.TemporalRecord;
import org.gbif.pipelines.transform.TemporalTransform;

import java.lang.reflect.Method;
import java.util.*;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.gbif.dwc.terms.DwcTerm.parentEventID;
import static org.gbif.pipelines.utils.ModelUtils.extractValue;

public class TemporalInterpretation {

  /** Transforms the source records into the location records using the geocode service. */
  public static Dataset<TemporalRecord> temporalTransform(
      Config config, SparkSession spark, Dataset<ExtendedRecord> source) {
    TemporalTransform temporalTransform =
        TemporalTransform.builder().build(); // TODO: add orderings from dataset tags

    // extract the location
    Dataset<RecordWithTemporal> recordWithTemporal =
        source.map(
            (MapFunction<ExtendedRecord, RecordWithTemporal>)
                er -> {
                  Temporal temporal = Temporal.buildFrom(er);
                  return RecordWithTemporal.builder()
                      .id(er.getId())
                      .coreId(er.getCoreId())
                      .parentId(extractValue(er, parentEventID))
                      .temporalHash(temporal.hash())
                      .temporal(temporal)
                      .build();
                },
            Encoders.bean(RecordWithTemporal.class));
    recordWithTemporal.createOrReplaceTempView("record_with_temporal");

    // distinct the locations to lookup
    Dataset<Temporal> distinctTemporals =
        spark
            .sql("SELECT DISTINCT temporal.* FROM record_with_temporal")
            .repartition(config.getTemporalParallelism())
            .as(Encoders.bean(Temporal.class));

    // lookup the distinct locations, and create a dictionary of the results
    Dataset<KeyedTemporalRecord> keyedLocation =
        distinctTemporals.map(
            (MapFunction<Temporal, KeyedTemporalRecord>)
                temporal -> {

                  // HACK - the function takes ExtendedRecord, but we have a Location
                  ExtendedRecord er =
                      ExtendedRecord.newBuilder()
                          .setId("UNUSED_BUT_NECESSARY")
                          .setCoreTerms(temporal.toCoreTermsMap())
                          .build();

                  // look them up
                  Optional<TemporalRecord> converted = temporalTransform.convert(er);
                  if (converted.isPresent())
                    return KeyedTemporalRecord.builder()
                        .key(temporal.hash())
                        .temporalRecord(converted.get())
                        .build();
                  else
                    return KeyedTemporalRecord.builder()
                        .key(temporal.hash())
                        .build(); // TODO: null handling?
                },
            Encoders.bean(KeyedTemporalRecord.class));
    keyedLocation.createOrReplaceTempView("key_temporal");

    // join the dictionary back to the source records
    Dataset<RecordWithTemporalRecord> expanded =
        spark
            .sql(
                "SELECT id, coreId, parentId, temporalRecord "
                    + "FROM record_with_temporal r "
                    + "  LEFT JOIN key_temporal l ON r.temporalHash = l.key")
            .as(Encoders.bean(RecordWithTemporalRecord.class));

    return expanded.map(
        (MapFunction<RecordWithTemporalRecord, TemporalRecord>)
            r -> {
              TemporalRecord temporalRecord =
                  r.getTemporalRecord() == null
                      ? TemporalRecord.newBuilder().build()
                      : r.getTemporalRecord();

              temporalRecord.setId(r.getId());
              temporalRecord.setCoreId(r.getCoreId());
              temporalRecord.setParentId(r.getParentId());
              return temporalRecord;
            },
        Encoders.bean(TemporalRecord.class));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecordWithTemporal {
    private String id;
    private String coreId;
    private String parentId;
    private String temporalHash;
    private Temporal temporal;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecordWithTemporalRecord {
    private String id;
    private String coreId;
    private String parentId;
    private TemporalRecord temporalRecord;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Temporal {
    private String id;
    private String day;
    private String month;
    private String year;
    private String modified;
    private String created;
    private String startDayOfYear;
    private String endDayOfYear;
    private String eventDate;
    private String dateIdentified;

    static Temporal buildFrom(ExtendedRecord er) {
      Temporal.TemporalBuilder builder = Temporal.builder();
      Arrays.asList(
              DwcTerm.day,
              DwcTerm.month,
              DwcTerm.year,
              DcTerm.modified,
              DcTerm.created,
              DwcTerm.startDayOfYear,
              DwcTerm.endDayOfYear,
              DwcTerm.eventDate,
              DwcTerm.dateIdentified)
          .forEach(
              term -> {
                String fieldName = term.simpleName(); // e.g., "country"
                String value =
                    er.getCoreTerms()
                        .get(term.qualifiedName()); // or however the ER provides values

                if (value != null) {
                  try {
                    Method setter = builder.getClass().getMethod(fieldName, String.class);
                    setter.invoke(builder, value);
                  } catch (NoSuchMethodException e) {
                    System.err.println("No setter for: " + fieldName);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              });

      return builder.build();
    }

    String hash() {
      return String.join(
          "|",
          id,
          day,
          month,
          year,
          modified,
          created,
          startDayOfYear,
          endDayOfYear,
          eventDate,
          dateIdentified);
    }

    public Map<String, String> toCoreTermsMap() {
      Map<String, String> coreTerms = new HashMap<>();

      if (getDay() != null) coreTerms.put(DwcTerm.day.qualifiedName(), getDay().toString());
      if (getMonth() != null) coreTerms.put(DwcTerm.month.qualifiedName(), getMonth().toString());
      if (getYear() != null) coreTerms.put(DwcTerm.year.qualifiedName(), getYear().toString());
      if (getModified() != null) coreTerms.put(DcTerm.modified.qualifiedName(), getModified());
      if (getCreated() != null)
        coreTerms.put(DcTerm.created.qualifiedName(), getCreated().toString());
      if (getStartDayOfYear() != null)
        coreTerms.put(DwcTerm.startDayOfYear.qualifiedName(), getStartDayOfYear().toString());
      if (getEndDayOfYear() != null)
        coreTerms.put(DwcTerm.endDayOfYear.qualifiedName(), getEndDayOfYear().toString());
      if (getEventDate() != null)
        coreTerms.put(DwcTerm.eventDate.qualifiedName(), getEventDate().toString());
      if (getDateIdentified() != null)
        coreTerms.put(DwcTerm.dateIdentified.qualifiedName(), getDateIdentified());

      return coreTerms;
    }
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KeyedTemporalRecord {
    private String key;
    private TemporalRecord temporalRecord;
  }
}
