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
package org.gbif.pipelines.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder", setterPrefix = "set")
public class GeologicalContext {
  private VocabularyConcept earliestEonOrLowestEonothem;
  private VocabularyConcept latestEonOrHighestEonothem;
  private VocabularyConcept earliestEraOrLowestErathem;
  private VocabularyConcept latestEraOrHighestErathem;
  private VocabularyConcept earliestPeriodOrLowestSystem;
  private VocabularyConcept latestPeriodOrHighestSystem;
  private VocabularyConcept earliestEpochOrLowestSeries;
  private VocabularyConcept latestEpochOrHighestSeries;
  private VocabularyConcept earliestAgeOrLowestStage;
  private VocabularyConcept latestAgeOrHighestStage;
  private String lowestBiostratigraphicZone;
  private String highestBiostratigraphicZone;
  private String group;
  private String formation;
  private String member;
  private String bed;
  private Float startAge;
  private Float endAge;
}
