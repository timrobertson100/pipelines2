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
import org.gbif.api.vocabulary.Extension;
import org.gbif.common.parsers.NumberParser;
import org.gbif.common.parsers.core.Parsable;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.dwc.terms.GbifTerm;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.parsers.identifiers.AgentIdentifierParser;
import org.gbif.pipelines.parsers.vocabulary.SimpleTypeParser;
import org.gbif.pipelines.parsers.vocabulary.VocabularyParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.gbif.api.vocabulary.Extension.*;
import static org.gbif.api.vocabulary.OccurrenceIssue.*;
import static org.gbif.pipelines.utils.ModelUtils.*;

/**
 * Interpreting function that receives a ExtendedRecord instance and applies an interpretation to
 * it.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BasicInterpreter {

  private static final Parsable<String> TYPE_NAME_PARSER =
      org.gbif.common.parsers.TypifiedNameParser.getInstance();

  /** {@link DwcTerm#individualCount} interpretation. */
  public static void interpretIndividualCount(ExtendedRecord er, BasicRecord br) {

    Consumer<Optional<Integer>> fn =
        parseResult -> {
          if (parseResult.isPresent()) {
            br.setIndividualCount(parseResult.get());
          } else {
            addIssue(br, INDIVIDUAL_COUNT_INVALID);
          }
        };

    SimpleTypeParser.parsePositiveInt(er, DwcTerm.individualCount, fn);
  }

  /** {@link DwcTerm#basisOfRecord} interpretation. */
  public static void interpretBasisOfRecord(ExtendedRecord er, BasicRecord br) {

    Function<ParseResult<BasisOfRecord>, BasicRecord> fn =
        parseResult -> {
          if (parseResult.isSuccessful()) {
            br.setBasisOfRecord(parseResult.getPayload().name());
          } else {
            br.setBasisOfRecord(BasisOfRecord.OCCURRENCE.name());
            addIssue(br, BASIS_OF_RECORD_INVALID);
          }
          return br;
        };

    VocabularyParser.basisOfRecordParser().map(er, fn);

    if (br.getBasisOfRecord() == null || br.getBasisOfRecord().isEmpty()) {
      br.setBasisOfRecord(BasisOfRecord.OCCURRENCE.name());
      addIssue(br, BASIS_OF_RECORD_INVALID);
    }
  }

  /** {@link GbifTerm#typifiedName} interpretation. */
  public static void interpretTypifiedName(ExtendedRecord er, BasicRecord br) {
    Optional<String> typifiedName = extractOptValue(er, GbifTerm.typifiedName);
    if (typifiedName.isPresent()) {
      br.setTypifiedName(typifiedName.get());
    } else {
      Optional.ofNullable(er.getCoreTerms().get(DwcTerm.typeStatus.qualifiedName()))
          .ifPresent(
              typeStatusValue -> {
                ParseResult<String> result =
                    TYPE_NAME_PARSER.parse(
                        er.getCoreTerms().get(DwcTerm.typeStatus.qualifiedName()));
                if (result.isSuccessful()) {
                  br.setTypifiedName(result.getPayload());
                }
              });
    }
  }

  /** {@link DwcTerm#organismQuantity} interpretation. */
  public static void interpretOrganismQuantity(ExtendedRecord er, BasicRecord br) {
    extractOptValue(er, DwcTerm.organismQuantity)
        .map(String::trim)
        .map(NumberParser::parseDouble)
        .filter(x -> !x.isInfinite() && !x.isNaN())
        .ifPresent(br::setOrganismQuantity);
  }

  /** {@link DwcTerm#organismQuantityType} interpretation. */
  public static void interpretOrganismQuantityType(ExtendedRecord er, BasicRecord br) {
    extractOptValue(er, DwcTerm.organismQuantityType)
        .map(String::trim)
        .ifPresent(br::setOrganismQuantityType);
  }

  /**
   * If the organism and sample have the same measure type, we can calculate relative organism
   * quantity
   */
  public static void interpretRelativeOrganismQuantity(BasicRecord br) {
    if (!Strings.isNullOrEmpty(br.getOrganismQuantityType())
        && !Strings.isNullOrEmpty(br.getSampleSizeUnit())
        && br.getOrganismQuantityType().equalsIgnoreCase(br.getSampleSizeUnit())) {
      Double organismQuantity = br.getOrganismQuantity();
      Double sampleSizeValue = br.getSampleSizeValue();
      if (organismQuantity != null && sampleSizeValue != null) {
        double result = organismQuantity / sampleSizeValue;
        if (!Double.isNaN(result) && !Double.isInfinite(result)) {
          br.setRelativeOrganismQuantity(organismQuantity / sampleSizeValue);
        }
      }
    }
  }

  /** {@link DwcTerm#identifiedByID}. */
  public static void interpretIdentifiedByIds(ExtendedRecord er, BasicRecord br) {
    extractOptValue(er, DwcTerm.identifiedByID)
        .filter(x -> !x.isEmpty())
        .map(AgentIdentifierParser::parse)
        .map(ArrayList::new)
        .ifPresent(br::setIdentifiedByIds);
  }

  /** {@link DwcTerm#recordedByID} interpretation. */
  public static void interpretRecordedByIds(ExtendedRecord er, BasicRecord br) {
    extractOptValue(er, DwcTerm.recordedByID)
        .filter(x -> !x.isEmpty())
        .map(AgentIdentifierParser::parse)
        .map(ArrayList::new)
        .ifPresent(br::setRecordedByIds);
  }

  /** {@link DwcTerm#otherCatalogNumbers} interpretation. */
  public static void interpretOtherCatalogNumbers(ExtendedRecord er, BasicRecord br) {
    List<String> list = extractListValue(DEFAULT_SEPARATOR + "|;", er, DwcTerm.otherCatalogNumbers);
    if (!list.isEmpty()) {
      br.setOtherCatalogNumbers(list);
    }
  }

  /** {@link DwcTerm#recordedBy} interpretation. */
  public static void interpretRecordedBy(ExtendedRecord er, BasicRecord br) {
    List<String> list = extractListValue(er, DwcTerm.recordedBy);
    if (!list.isEmpty()) {
      br.setRecordedBy(list);
    }
  }

  /** {@link DwcTerm#identifiedBy} interpretation. */
  public static void interpretIdentifiedBy(ExtendedRecord er, BasicRecord br) {
    List<String> list = extractListValue(er, DwcTerm.identifiedBy);
    if (!list.isEmpty()) {
      br.setIdentifiedBy(list);
    }
  }

  /** {@link DwcTerm#preparations} interpretation. */
  public static void interpretPreparations(ExtendedRecord er, BasicRecord br) {
    List<String> list = extractListValue(er, DwcTerm.preparations);
    if (!list.isEmpty()) {
      br.setPreparations(list);
    }
  }

  /** {@link GbifTerm#projectId} interpretation. */
  public static void interpretProjectId(ExtendedRecord er, BasicRecord br) {
    List<String> list = extractListValue(er, GbifTerm.projectId);
    if (!list.isEmpty()) {
      br.setProjectId(list);
    }
  }

  /** {@link DwcTerm#associatedSequences} interpretation. */
  public static void interpretIsSequenced(ExtendedRecord er, BasicRecord br) {

    boolean hasExt = false;
    var extensions = er.getExtensions();

    if (extensions != null) {
      Predicate<Extension> fn =
          ext -> {
            var e = extensions.get(ext.getRowType());
            return e != null && !e.isEmpty();
          };
      hasExt = fn.test(DNA_DERIVED_DATA);
      hasExt = fn.test(AMPLIFICATION) || hasExt;
      hasExt = fn.test(CLONING) || hasExt;
      hasExt = fn.test(GEL_IMAGE) || hasExt;
    }

    boolean hasAssociatedSequences =
        extractNullAwareOptValue(er, DwcTerm.associatedSequences).isPresent();

    br.setIsSequenced(hasExt || hasAssociatedSequences);
  }

  /** {@link DwcTerm#associatedSequences} interpretation. */
  public static void interpretAssociatedSequences(ExtendedRecord er, BasicRecord br) {
    List<String> list = extractListValue(DEFAULT_SEPARATOR + "|;", er, DwcTerm.associatedSequences);
    if (!list.isEmpty()) {
      br.setAssociatedSequences(list);
    }
  }

  /** Sets the coreId field. */
  public static void setCoreId(ExtendedRecord er, BasicRecord br) {
    Optional.ofNullable(er.getCoreId()).ifPresent(br::setCoreId);
  }
}
