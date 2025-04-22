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
@NoArgsConstructor
@AllArgsConstructor
@Builder(builderClassName = "Builder", builderMethodName = "newBuilder", setterPrefix = "set")
public class TemporalRecord implements Issues {
  private String id;
  private String coreId;
  private String parentId;
  private Long created;
  private Integer year;
  private Integer month;
  private Integer day;
  private EventDate eventDate;
  private Integer startDayOfYear;
  private Integer endDayOfYear;
  private String modified;
  private String dateIdentified;
  private String datePrecision;
  @lombok.Builder.Default private IssueRecord issues = IssueRecord.newBuilder().build();
}
