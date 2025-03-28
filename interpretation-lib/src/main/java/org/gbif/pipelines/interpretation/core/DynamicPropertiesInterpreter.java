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

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.pipelines.models.BasicRecord;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.parsers.vertnet.LifeStageParser;
import org.gbif.pipelines.parsers.vertnet.SexParser;
import org.gbif.pipelines.parsers.vocabulary.VocabularyService;
import org.gbif.pipelines.utils.VocabularyConceptFactory;

import java.util.function.BiConsumer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
