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

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder", setterPrefix = "set")
public class BasicRecord implements Issues {
  private String id;
  private String coreId;
  private Long created;
  private String basisOfRecord;
  private VocabularyConcept sex;
  private VocabularyConcept lifeStage;
  private VocabularyConcept establishmentMeans;
  private VocabularyConcept degreeOfEstablishment;
  private VocabularyConcept pathway;
  private Integer individualCount;
  private List<VocabularyConcept> typeStatus;
  private String typifiedName;
  private Double sampleSizeValue;
  private String sampleSizeUnit;
  private Double organismQuantity;
  private String organismQuantityType;
  private Double relativeOrganismQuantity;
  private String references;
  private String license;
  private List<AgentIdentifier> identifiedByIds;
  private List<String> identifiedBy;
  private List<AgentIdentifier> recordedByIds;
  private List<String> recordedBy;
  private String occurrenceStatus;
  private List<String> datasetID;
  private List<String> datasetName;
  private List<String> otherCatalogNumbers;
  private List<String> preparations;
  private List<String> samplingProtocol;
  private List<String> projectId;
  private GeologicalContext geologicalContext;
  private Boolean isSequenced;
  private List<String> associatedSequences;
  @lombok.Builder.Default private IssueRecord issues = IssueRecord.newBuilder().build();
}
