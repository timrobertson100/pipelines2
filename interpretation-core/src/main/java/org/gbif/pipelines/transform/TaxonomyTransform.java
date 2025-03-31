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
package org.gbif.pipelines.transform;

import org.gbif.api.vocabulary.Kingdom;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.kvs.KeyValueStore;
import org.gbif.kvs.species.Identification;
import org.gbif.nameparser.NameParserGBIF;
import org.gbif.nameparser.NameParserGbifV1;
import org.gbif.nameparser.api.NameParser;
import org.gbif.nameparser.api.UnparsableNameException;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.models.taxonomy.*;
import org.gbif.pipelines.parsers.taxonomy.TaxonRecordConverter;
import org.gbif.pipelines.utils.IdentificationUtils;
import org.gbif.pipelines.utils.SpeciesMatchKVSFactory;
import org.gbif.rest.client.species.NameUsageMatch;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.api.vocabulary.OccurrenceIssue.TAXON_MATCH_FUZZY;
import static org.gbif.api.vocabulary.OccurrenceIssue.TAXON_MATCH_HIGHERRANK;
import static org.gbif.api.vocabulary.OccurrenceIssue.TAXON_MATCH_NONE;
import static org.gbif.pipelines.utils.ModelUtils.*;
import static org.gbif.pipelines.utils.ModelUtils.extractNullAwareOptValue;
import static org.gbif.pipelines.utils.ModelUtils.extractValue;

/** Performs the lookup against the species API. */
@Slf4j
@Builder
public class TaxonomyTransform implements Serializable {
  private String speciesApiUrl;
  private static final RankedName INCERTAE_SEDIS =
      RankedName.newBuilder()
          .setRank(Rank.KINGDOM)
          .setName(Kingdom.INCERTAE_SEDIS.scientificName())
          .setKey(Kingdom.INCERTAE_SEDIS.nubUsageKey())
          .build();
  private static final NameParser NAME_PARSER = new NameParserGBIF();

  public static Identification extractIdentification(ExtendedRecord er) {
    Map<String, String> termsSource = IdentificationUtils.getIdentificationFieldTermsSource(er);

    // https://github.com/gbif/portal-feedback/issues/4231
    String scientificName =
        extractNullAwareOptValue(termsSource, DwcTerm.scientificName)
            .orElse(extractValue(termsSource, DwcTerm.verbatimIdentification));

    return Identification.builder()
        .withKingdom(extractValue(termsSource, DwcTerm.kingdom))
        .withPhylum(extractValue(termsSource, DwcTerm.phylum))
        .withClazz(extractValue(termsSource, DwcTerm.class_))
        .withOrder(extractValue(termsSource, DwcTerm.order))
        .withFamily(extractValue(termsSource, DwcTerm.family))
        .withGenus(extractValue(termsSource, DwcTerm.genus))
        .withScientificName(scientificName)
        .withGenericName(extractValue(termsSource, DwcTerm.genericName))
        .withSpecificEpithet(extractValue(termsSource, DwcTerm.specificEpithet))
        .withInfraspecificEpithet(extractValue(termsSource, DwcTerm.infraspecificEpithet))
        .withScientificNameAuthorship(extractValue(termsSource, DwcTerm.scientificNameAuthorship))
        .withRank(extractValue(termsSource, DwcTerm.taxonRank))
        .withVerbatimRank(extractValue(termsSource, DwcTerm.verbatimTaxonRank))
        .withScientificNameID(extractValue(termsSource, DwcTerm.scientificNameID))
        .withTaxonID(extractValue(termsSource, DwcTerm.taxonID))
        .withTaxonConceptID(extractValue(termsSource, DwcTerm.taxonConceptID))
        .build();
  }

  /** Looks up the identification using the species API, preserving the source hash. */
  public Optional<TaxonRecord> lookup(Identification identification) throws IOException {
    KeyValueStore<Identification, NameUsageMatch> kvStore =
        SpeciesMatchKVSFactory.getKvStore(speciesApiUrl);
    TaxonRecord tr = TaxonRecord.newBuilder().build();
    NameUsageMatch usageMatch;
    try {
      usageMatch = kvStore.get(identification);
    } catch (Exception ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex.getMessage());
    }

    if (usageMatch == null || isEmpty(usageMatch) || checkFuzzy(usageMatch, identification)) {
      // "NO_MATCHING_RESULTS". This
      // happens when we get an empty response from the WS
      addIssue(tr, TAXON_MATCH_NONE);
      tr.setUsage(INCERTAE_SEDIS);
      tr.setClassification(Collections.singletonList(INCERTAE_SEDIS));
    } else {

      org.gbif.api.model.checklistbank.NameUsageMatch.MatchType matchType =
          usageMatch.getDiagnostics().getMatchType();

      // copy any issues asserted by the lookup itself
      if (usageMatch.getIssues() != null) {
        addIssueSet(tr, usageMatch.getIssues());
      }

      if (org.gbif.api.model.checklistbank.NameUsageMatch.MatchType.NONE == matchType) {
        addIssue(tr, TAXON_MATCH_NONE);
      } else if (org.gbif.api.model.checklistbank.NameUsageMatch.MatchType.FUZZY == matchType) {
        addIssue(tr, TAXON_MATCH_FUZZY);
      } else if (org.gbif.api.model.checklistbank.NameUsageMatch.MatchType.HIGHERRANK
          == matchType) {
        addIssue(tr, TAXON_MATCH_HIGHERRANK);
      }

      // parse name into pieces - we don't get them from the nub lookup
      try {
        if (Objects.nonNull(usageMatch.getUsage())) {
          org.gbif.nameparser.api.ParsedName pn =
              NAME_PARSER.parse(
                  usageMatch.getUsage().getName(),
                  NameParserGbifV1.fromGbif(usageMatch.getUsage().getRank()),
                  null);
          tr.setUsageParsedName(toParsedName(pn));
        }
      } catch (UnparsableNameException e) {
        if (e.getType().isParsable()) {
          log.warn(
              "Fail to parse backbone {} name for source: {}",
              e.getType(),
              usageMatch.getUsage().getName());
        }
      } catch (InterruptedException e) {
        log.warn(
            "Parsing backbone name failed with interruption  {}", usageMatch.getUsage().getName());
      }

      // convert taxon record
      TaxonRecordConverter.convert(usageMatch, tr);
    }
    return Optional.of(tr);
  }

  private static boolean isEmpty(NameUsageMatch response) {
    return response == null
        || response.getUsage() == null
        || (response.getClassification() == null || response.getClassification().isEmpty())
        || response.getDiagnostics() == null;
  }

  /**
   * To be able to return NONE, if response is FUZZY and higher taxa is null or empty Fix for
   * https://github.com/gbif/pipelines/issues/254
   */
  @VisibleForTesting
  protected static boolean checkFuzzy(NameUsageMatch usageMatch, Identification identification) {
    boolean isFuzzy =
        org.gbif.api.model.checklistbank.NameUsageMatch.MatchType.FUZZY
            == usageMatch.getDiagnostics().getMatchType();
    boolean isEmptyTaxa =
        Strings.isNullOrEmpty(identification.getKingdom())
            && Strings.isNullOrEmpty(identification.getPhylum())
            && Strings.isNullOrEmpty(identification.getClazz())
            && Strings.isNullOrEmpty(identification.getOrder())
            && Strings.isNullOrEmpty(identification.getFamily());
    return isFuzzy && isEmptyTaxa;
  }

  private static ParsedName toParsedName(org.gbif.nameparser.api.ParsedName pn) {
    ParsedName.Builder builder =
        ParsedName.newBuilder()
            .setAbbreviated(pn.isAbbreviated())
            .setAutonym(pn.isAutonym())
            .setBinomial(pn.isBinomial())
            .setCandidatus(pn.isCandidatus())
            .setCultivarEpithet(pn.getCultivarEpithet())
            .setDoubtful(pn.isDoubtful())
            .setGenus(pn.getGenus())
            .setUninomial(pn.getUninomial())
            .setUnparsed(pn.getUnparsed())
            .setTrinomial(pn.isTrinomial())
            .setIncomplete(pn.isIncomplete())
            .setIndetermined(pn.isIndetermined())
            .setTerminalEpithet(pn.getTerminalEpithet())
            .setInfragenericEpithet(pn.getInfragenericEpithet())
            .setInfraspecificEpithet(pn.getInfraspecificEpithet())
            .setExtinct(pn.isExtinct())
            .setPublishedIn(pn.getPublishedIn())
            .setSanctioningAuthor(pn.getSanctioningAuthor())
            .setSpecificEpithet(pn.getSpecificEpithet())
            .setPhrase(pn.getPhrase())
            .setPhraseName(pn.isPhraseName())
            .setVoucher(pn.getVoucher())
            .setNominatingParty(pn.getNominatingParty())
            .setNomenclaturalNote(pn.getNomenclaturalNote());

    // Nullable fields
    Optional.ofNullable(pn.getWarnings())
        .ifPresent(w -> builder.setWarnings(new ArrayList<>(pn.getWarnings())));
    Optional.ofNullable(pn.getBasionymAuthorship())
        .ifPresent(authorship -> builder.setBasionymAuthorship(toAuthorship(authorship)));
    Optional.ofNullable(pn.getCombinationAuthorship())
        .ifPresent(authorship -> builder.setCombinationAuthorship(toAuthorship(authorship)));
    Optional.ofNullable(pn.getCode())
        .ifPresent(code -> builder.setCode(NomCode.valueOf(code.name())));
    Optional.ofNullable(pn.getType())
        .ifPresent(type -> builder.setType(NameType.valueOf(type.name())));
    Optional.ofNullable(pn.getNotho())
        .ifPresent(notho -> builder.setNotho(NamePart.valueOf(notho.name())));
    Optional.ofNullable(pn.getRank())
        .ifPresent(rank -> builder.setRank(NameRank.valueOf(rank.name())));
    Optional.ofNullable(pn.getState())
        .ifPresent(state -> builder.setState(State.valueOf(state.name())));
    Optional.ofNullable(pn.getEpithetQualifier())
        .map(
            eq ->
                eq.entrySet().stream()
                    .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue)))
        .ifPresent(builder::setEpithetQualifier);
    return builder.build();
  }

  private static Authorship toAuthorship(org.gbif.nameparser.api.Authorship authorship) {
    return Authorship.newBuilder()
        .setEmpty(authorship.isEmpty())
        .setYear(authorship.getYear())
        .setAuthors(authorship.getAuthors())
        .setExAuthors(authorship.getExAuthors())
        .build();
  }
}
