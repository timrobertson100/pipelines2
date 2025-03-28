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
package org.gbif.pipelines.interpretation.core;

import org.gbif.api.vocabulary.BasisOfRecord;
import org.gbif.api.vocabulary.OccurrenceIssue;
import org.gbif.api.vocabulary.OccurrenceStatus;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.Term;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.EventCoreRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.models.VocabularyConcept;
import org.gbif.pipelines.parsers.vocabulary.SimpleTypeParser;
import org.gbif.pipelines.parsers.vocabulary.VocabularyService;
import org.gbif.pipelines.utils.ModelUtils;
import org.gbif.pipelines.utils.VocabularyConceptFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.api.vocabulary.OccurrenceIssue.*;
import static org.gbif.pipelines.utils.ModelUtils.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VocabularyInterpreter {

  /** Values taken from <a href="https://github.com/gbif/vocabulary/issues/87">here</a> */
  private static final Set<String> SUSPECTED_TYPE_STATUS_VALUES =
      Set.of("?", "possible", "possibly", "potential", "maybe", "perhaps");

  /** {@link DwcTerm#lifeStage} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretLifeStage(
      VocabularyService vocabularyService) {
    return (er, br) ->
        interpretVocabulary(er, DwcTerm.lifeStage, vocabularyService).ifPresent(br::setLifeStage);
  }

  /** {@link DwcTerm#establishmentMeans} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretEstablishmentMeans(
      VocabularyService vocabularyService) {
    return (er, br) ->
        interpretVocabulary(er, DwcTerm.establishmentMeans, vocabularyService)
            .ifPresent(br::setEstablishmentMeans);
  }

  /** {@link DwcTerm#degreeOfEstablishment} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretDegreeOfEstablishment(
      VocabularyService vocabularyService) {
    return (er, br) ->
        interpretVocabulary(er, DwcTerm.degreeOfEstablishment, vocabularyService)
            .ifPresent(br::setDegreeOfEstablishment);
  }

  /** {@link DwcTerm#pathway} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretPathway(
      VocabularyService vocabularyService) {
    return (er, br) ->
        interpretVocabulary(er, DwcTerm.pathway, vocabularyService).ifPresent(br::setPathway);
  }

  /** {@link DwcTerm#pathway} interpretation. */
  public static BiConsumer<ExtendedRecord, EventCoreRecord> interpretEventType(
      VocabularyService vocabularyService) {
    return (er, ecr) ->
        interpretVocabulary(er, DwcTerm.eventType, vocabularyService).ifPresent(ecr::setEventType);
  }

  /** {@link DwcTerm#typeStatus} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretTypeStatus(
      VocabularyService vocabularyService) {
    return (er, br) ->
        extractListValue(er, DwcTerm.typeStatus)
            .forEach(
                value ->
                    interpretVocabulary(
                            DwcTerm.typeStatus,
                            value,
                            vocabularyService,
                            v -> {
                              if (SUSPECTED_TYPE_STATUS_VALUES.stream()
                                  .anyMatch(sts -> v.toLowerCase().contains(sts))) {
                                addIssue(br, OccurrenceIssue.SUSPECTED_TYPE);
                              } else {
                                addIssue(br, OccurrenceIssue.TYPE_STATUS_INVALID);
                              }
                            })
                        .ifPresent(
                            v -> {
                              if (br.getTypeStatus() == null) {
                                br.setTypeStatus(new ArrayList<>());
                              }
                              br.getTypeStatus().add(v);
                            }));
  }

  /** {@link DwcTerm#sex} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretSex(
      VocabularyService vocabularyService) {
    return (er, br) ->
        interpretVocabulary(er, DwcTerm.sex, vocabularyService).ifPresent(br::setSex);
  }

  /** {@link DwcTerm#occurrenceStatus} interpretation. */
  public static BiConsumer<ExtendedRecord, BasicRecord> interpretOccurrenceStatus(
      VocabularyService vocabularyService) {
    return (er, br) -> {
      String rawCount = ModelUtils.extractNullAwareValue(er, DwcTerm.individualCount);
      Integer parsedCount = SimpleTypeParser.parsePositiveIntOpt(rawCount).orElse(null);

      String rawOccStatus = ModelUtils.extractNullAwareValue(er, DwcTerm.occurrenceStatus);

      Optional<VocabularyConcept> os =
          interpretVocabulary(er, DwcTerm.occurrenceStatus, vocabularyService);
      OccurrenceStatus parsedOccStatus =
          rawOccStatus != null && os.isPresent()
              ? OccurrenceStatus.valueOf(os.get().getConcept())
              : null;

      boolean isCountNull = rawCount == null;
      boolean isCountRubbish = rawCount != null && parsedCount == null;
      boolean isCountZero = parsedCount != null && parsedCount == 0;
      boolean isCountGreaterZero = parsedCount != null && parsedCount > 0;

      boolean isOccNull = rawOccStatus == null;
      boolean isOccPresent = parsedOccStatus == OccurrenceStatus.PRESENT;
      boolean isOccAbsent = parsedOccStatus == OccurrenceStatus.ABSENT;
      boolean isOccRubbish = parsedOccStatus == null;

      // https://github.com/gbif/pipelines/issues/392
      boolean isSpecimen =
          Optional.ofNullable(br.getBasisOfRecord())
              .map(BasisOfRecord::valueOf)
              .map(
                  x ->
                      x == BasisOfRecord.PRESERVED_SPECIMEN
                          || x == BasisOfRecord.FOSSIL_SPECIMEN
                          || x == BasisOfRecord.LIVING_SPECIMEN)
              .orElse(false);

      // rawCount === null
      if (isCountNull) {
        if (isOccNull || isOccPresent) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
        } else if (isOccAbsent) {
          br.setOccurrenceStatus(OccurrenceStatus.ABSENT.name());
        } else if (isOccRubbish) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
          addIssue(br, OCCURRENCE_STATUS_UNPARSABLE);
        }
      } else if (isCountRubbish) {
        if (isOccNull || isOccPresent) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
        } else if (isOccAbsent) {
          br.setOccurrenceStatus(OccurrenceStatus.ABSENT.name());
        } else if (isOccRubbish) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
          addIssue(br, OCCURRENCE_STATUS_UNPARSABLE);
        }
        addIssue(br, INDIVIDUAL_COUNT_INVALID);
      } else if (isCountZero) {
        if (isOccNull && isSpecimen) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
          addIssue(br, OCCURRENCE_STATUS_INFERRED_FROM_BASIS_OF_RECORD);
        } else if (isOccNull) {
          br.setOccurrenceStatus(OccurrenceStatus.ABSENT.name());
          addIssue(br, OCCURRENCE_STATUS_INFERRED_FROM_INDIVIDUAL_COUNT);
        } else if (isOccPresent) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
          addIssue(br, INDIVIDUAL_COUNT_CONFLICTS_WITH_OCCURRENCE_STATUS);
        } else if (isOccAbsent) {
          br.setOccurrenceStatus(OccurrenceStatus.ABSENT.name());
        } else if (isOccRubbish) {
          br.setOccurrenceStatus(OccurrenceStatus.ABSENT.name());
          addIssue(br, OCCURRENCE_STATUS_UNPARSABLE);
          addIssue(br, OCCURRENCE_STATUS_INFERRED_FROM_INDIVIDUAL_COUNT);
        }
      } else if (isCountGreaterZero) {
        if (isOccNull) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
          addIssue(br, OCCURRENCE_STATUS_INFERRED_FROM_INDIVIDUAL_COUNT);
        } else if (isOccPresent) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
        } else if (isOccAbsent) {
          br.setOccurrenceStatus(OccurrenceStatus.ABSENT.name());
          addIssue(br, INDIVIDUAL_COUNT_CONFLICTS_WITH_OCCURRENCE_STATUS);
        } else if (isOccRubbish) {
          br.setOccurrenceStatus(OccurrenceStatus.PRESENT.name());
          addIssue(br, OCCURRENCE_STATUS_UNPARSABLE);
          addIssue(br, OCCURRENCE_STATUS_INFERRED_FROM_INDIVIDUAL_COUNT);
        }
      }
    };
  }

  private static Optional<VocabularyConcept> interpretVocabulary(
      ExtendedRecord er, Term term, VocabularyService vocabularyService) {
    return interpretVocabulary(term, extractNullAwareValue(er, term), vocabularyService, null);
  }

  static Optional<VocabularyConcept> interpretVocabulary(
      Term term, String value, VocabularyService vocabularyService) {
    return interpretVocabulary(term, value, vocabularyService, null);
  }

  private static Optional<VocabularyConcept> interpretVocabulary(
      ExtendedRecord er, Term term, VocabularyService vocabularyService, Consumer<String> issueFn) {
    return interpretVocabulary(term, extractNullAwareValue(er, term), vocabularyService, issueFn);
  }

  static Optional<VocabularyConcept> interpretVocabulary(
      Term term, String value, VocabularyService vocabularyService, Consumer<String> issueFn) {
    if (vocabularyService == null) {
      return Optional.empty();
    }

    if (value != null) {
      Optional<VocabularyConcept> result =
          vocabularyService
              .get(term)
              .flatMap(lookup -> Optional.of(value).flatMap(lookup::lookup))
              .map(VocabularyConceptFactory::createConcept);
      if (result.isEmpty() && issueFn != null) {
        issueFn.accept(value);
      }
      return result;
    }

    return Optional.empty();
  }
}
