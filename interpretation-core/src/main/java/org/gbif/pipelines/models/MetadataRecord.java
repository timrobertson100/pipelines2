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
public class MetadataRecord implements Issues {
  private String id;
  private Long created;
  private Long lastCrawled;
  private String datasetKey;
  private Integer crawlId;
  private String datasetTitle;
  private String installationKey;
  private String publisherTitle;
  private String publishingOrganizationKey;
  private String endorsingNodeKey;
  private String projectId;
  private String programmeAcronym;
  private String protocol;
  private String license;
  private String datasetPublishingCountry;
  private String hostingOrganizationKey;
  private List<String> networkKeys;
  private List<MachineTag> machineTags;
  @lombok.Builder.Default private IssueRecord issues = IssueRecord.newBuilder().build();
}
