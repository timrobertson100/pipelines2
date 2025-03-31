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

import org.gbif.pipelines.models.IssueRecord;
import org.gbif.pipelines.models.Issues;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(
    builderClassName = "Builder",
    builderMethodName = "newBuilder",
    setterPrefix = "set",
    toBuilder = true)
public class TaxonRecord implements Issues {
  private String id;
  private String coreId;
  private String parentId;
  private Long created;
  private Boolean synonym;
  private RankedName usage;
  private List<RankedName> classification;
  private RankedName acceptedUsage;
  private Nomenclature nomenclature;
  private Diagnostic diagnostics;
  private ParsedName usageParsedName;
  @lombok.Builder.Default private IssueRecord issues = IssueRecord.newBuilder().build();
  private String iucnRedListCategoryCode;
}
