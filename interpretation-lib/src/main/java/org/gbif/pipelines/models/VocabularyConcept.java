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
public class VocabularyConcept {
  private String concept;
  private List<String> lineage;
  private List<VocabularyTag> tags;
}
