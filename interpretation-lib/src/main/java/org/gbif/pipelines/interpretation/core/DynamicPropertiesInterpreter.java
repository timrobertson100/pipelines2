package org.gbif.pipelines.interpretation.core;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.pipelines.parsers.vertnet.LifeStageParser;
import org.gbif.pipelines.parsers.vertnet.SexParser;
import org.gbif.pipelines.parsers.vocabulary.VocabularyService;
import org.gbif.pipelines.utils.VocabularyConceptFactory;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;

import java.util.function.BiConsumer;

import static org.gbif.pipelines.utils.ModelUtils.extractNullAwareOptValue;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DynamicPropertiesInterpreter {

  public static BiConsumer<ExtendedRecord, BasicRecord> interpretSex(
      VocabularyService vocabularyService) {
    return (er, br) -> {
      if (br.getSex() == null) {
        vocabularyService
            .get(DwcTerm.sex)
            .flatMap(
                lookup ->
                    extractNullAwareOptValue(er, DwcTerm.dynamicProperties)
                        .flatMap(v -> SexParser.parse(v).flatMap(lookup::lookup)))
            .map(VocabularyConceptFactory::createConcept)
            .ifPresent(br::setSex);
      }
    };
  }

  public static BiConsumer<ExtendedRecord, BasicRecord> interpretLifeStage(
      VocabularyService vocabularyService) {
    return (er, br) -> {
      if (br.getLifeStage() == null) {
        vocabularyService
            .get(DwcTerm.lifeStage)
            .flatMap(
                lookup ->
                    extractNullAwareOptValue(er, DwcTerm.dynamicProperties)
                        .flatMap(v -> LifeStageParser.parse(v).flatMap(lookup::lookup)))
            .map(VocabularyConceptFactory::createConcept)
            .ifPresent(br::setLifeStage);
      }
    };
  }
}
