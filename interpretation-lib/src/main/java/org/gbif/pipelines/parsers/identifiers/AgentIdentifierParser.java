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
package org.gbif.pipelines.parsers.identifiers;

import org.gbif.api.util.validators.identifierschemes.OrcidValidator;
import org.gbif.api.util.validators.identifierschemes.OtherValidator;
import org.gbif.api.util.validators.identifierschemes.WikidataValidator;
import org.gbif.api.vocabulary.AgentIdentifierType;
import org.gbif.pipelines.models.AgentIdentifier;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentIdentifierParser {

  private static final OrcidValidator ORCID_VALIDATOR = new OrcidValidator();
  private static final WikidataValidator WIKIDATA_VALIDATOR = new WikidataValidator();
  private static final OtherValidator OTHER_VALIDATOR = new OtherValidator();

  private static final String DELIMITER = "[|,]";

  public static Set<AgentIdentifier> parse(String raw) {
    if (Strings.isNullOrEmpty(raw)) {
      return Collections.emptySet();
    }
    return Stream.of(raw.split(DELIMITER))
        .map(String::trim)
        .filter(x -> !x.isEmpty())
        .map(AgentIdentifierParser::parseValue)
        .collect(Collectors.toSet());
  }

  private static AgentIdentifier parseValue(String raw) {
    if (ORCID_VALIDATOR.isValid(raw)) {
      return AgentIdentifier.newBuilder()
          .setType(AgentIdentifierType.ORCID.name())
          .setValue(ORCID_VALIDATOR.normalize(raw))
          .build();
    }
    if (WIKIDATA_VALIDATOR.isValid(raw)) {
      return AgentIdentifier.newBuilder()
          .setType(AgentIdentifierType.WIKIDATA.name())
          .setValue(WIKIDATA_VALIDATOR.normalize(raw))
          .build();
    }
    return AgentIdentifier.newBuilder()
        .setType(AgentIdentifierType.OTHER.name())
        .setValue(OTHER_VALIDATOR.normalize(raw))
        .build();
  }
}
