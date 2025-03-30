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
