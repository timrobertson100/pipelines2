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
public class EventCoreRecord implements Issues {
  private java.lang.String id;
  private java.lang.String parentEventID;
  private VocabularyConcept eventType;
  private java.lang.Long created;
  private java.lang.Double sampleSizeValue;
  private java.lang.String sampleSizeUnit;
  private java.lang.String references;
  private java.lang.String license;
  private List<String> datasetID;
  private List<java.lang.String> datasetName;
  private List<java.lang.String> samplingProtocol;
  private List<Parent> parentsLineage;
  private java.lang.String locationID;
  private IssueRecord issues;
}
