package org.gbif.pipelines.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
  private IssueRecord issues;
}
