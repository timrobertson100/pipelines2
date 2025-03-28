package org.gbif.pipelines.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder", setterPrefix = "set")
public class ExtendedRecord implements Serializable {
  private String id;
  private String coreId;
  private String coreRowType;
  private Map<String, String> coreTerms;
  private Map<String, List<Map<String, String>>> extensions;
}
