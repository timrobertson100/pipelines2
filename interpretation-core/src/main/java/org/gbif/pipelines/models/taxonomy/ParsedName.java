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
package org.gbif.pipelines.models.taxonomy;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder", setterPrefix = "set")
public class ParsedName {
  private Boolean abbreviated;
  private Boolean autonym;
  private Authorship basionymAuthorship;
  private Boolean binomial;
  private Boolean candidatus;
  private NomCode code;
  private Boolean extinct;
  private String publishedIn;
  private Authorship combinationAuthorship;
  private String cultivarEpithet;
  private String phrase;
  private Boolean phraseName;
  private String voucher;
  private String nominatingParty;
  private Boolean doubtful;
  private String genus;
  private Boolean incomplete;
  private Boolean indetermined;
  private String infragenericEpithet;
  private String infraspecificEpithet;
  private String nomenclaturalNote;
  private NamePart notho;
  private Map<String, String> epithetQualifier;
  private NameRank rank;
  private String sanctioningAuthor;
  private String specificEpithet;
  private State state;
  private String terminalEpithet;
  private Boolean trinomial;
  private NameType type;
  private String uninomial;
  private String unparsed;
  private List<String> warnings;
}
