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
package org.gbif.pipelines.parsers.taxonomy;

import org.gbif.pipelines.models.taxonomy.*;
import org.gbif.rest.client.species.IucnRedListCategory;
import org.gbif.rest.client.species.NameUsageMatch;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Adapts a {@link NameUsageMatch} into a {@link TaxonRecord} */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TaxonRecordConverter {

  /**
   * I modify the parameter instead of creating a new one and returning it because the lambda
   * parameters are final used in Interpreter.
   */
  public static void convert(NameUsageMatch nameUsageMatch, TaxonRecord taxonRecord) {
    Objects.requireNonNull(nameUsageMatch);
    convertInternal(nameUsageMatch, taxonRecord);
  }

  private static TaxonRecord convertInternal(NameUsageMatch source, TaxonRecord taxonRecord) {

    List<RankedName> classifications =
        source.getClassification().stream()
            .map(TaxonRecordConverter::convertRankedName)
            .collect(Collectors.toList());

    taxonRecord.setClassification(classifications);
    taxonRecord.setSynonym(source.isSynonym());
    taxonRecord.setUsage(convertRankedName(source.getUsage()));
    // Usage is set as the accepted usage if the accepted usage is null
    taxonRecord.setAcceptedUsage(
        Optional.ofNullable(convertRankedName(source.getAcceptedUsage()))
            .orElse(taxonRecord.getUsage()));
    taxonRecord.setNomenclature(convertNomenclature(source.getNomenclature()));
    taxonRecord.setDiagnostics(convertDiagnostics(source.getDiagnostics()));

    // IUCN Red List Category
    Optional.ofNullable(source.getIucnRedListCategory())
        .map(IucnRedListCategory::getCode)
        .ifPresent(taxonRecord::setIucnRedListCategoryCode);

    return taxonRecord;
  }

  private static RankedName convertRankedName(org.gbif.api.v2.RankedName rankedNameApi) {
    if (rankedNameApi == null) {
      return null;
    }

    return RankedName.newBuilder()
        .setKey(rankedNameApi.getKey())
        .setName(rankedNameApi.getName())
        .setRank(Rank.valueOf(rankedNameApi.getRank().name()))
        .build();
  }

  private static Nomenclature convertNomenclature(NameUsageMatch.Nomenclature nomenclatureApi) {
    if (nomenclatureApi == null) {
      return null;
    }

    return Nomenclature.newBuilder()
        .setId(nomenclatureApi.getId())
        .setSource(nomenclatureApi.getSource())
        .build();
  }

  private static Diagnostic convertDiagnostics(NameUsageMatch.Diagnostics diagnosticsApi) {
    if (diagnosticsApi == null) {
      return null;
    }

    // alternatives
    List<TaxonRecord> alternatives =
        diagnosticsApi.getAlternatives().stream()
            .map(match -> convertInternal(match, TaxonRecord.newBuilder().build()))
            .collect(Collectors.toList());

    Diagnostic.Builder builder =
        Diagnostic.newBuilder()
            // .setAlternatives(alternatives) // circular references!!!
            .setConfidence(diagnosticsApi.getConfidence())
            .setMatchType(MatchType.valueOf(diagnosticsApi.getMatchType().name()))
            .setNote(diagnosticsApi.getNote())
            .setLineage(diagnosticsApi.getLineage());

    // status. A bit of defensive programming...
    if (diagnosticsApi.getStatus() != null) {
      builder.setStatus(Status.valueOf(diagnosticsApi.getStatus().name()));
    }

    return builder.build();
  }
}
