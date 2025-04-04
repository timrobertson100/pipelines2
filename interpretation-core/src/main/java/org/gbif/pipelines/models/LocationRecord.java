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
public class LocationRecord implements Issues {
  private String id;
  private String coreId;
  private String parentId;
  private Long created;
  private String continent;
  private String waterBody;
  private String country;
  private String countryCode;
  private String publishingCountry;
  private String gbifRegion;
  private String publishedByGbifRegion;
  private String stateProvince;
  private Double minimumElevationInMeters;
  private Double maximumElevationInMeters;
  private Double elevation;
  private Double elevationAccuracy;
  private Double minimumDepthInMeters;
  private Double maximumDepthInMeters;
  private Double depth;
  private Double depthAccuracy;
  private Double minimumDistanceAboveSurfaceInMeters;
  private Double maximumDistanceAboveSurfaceInMeters;
  private Double decimalLatitude;
  private Double decimalLongitude;
  private Double coordinateUncertaintyInMeters;
  private Double coordinatePrecision;
  private Boolean hasCoordinate;
  private Boolean repatriated;
  private Boolean hasGeospatialIssue;
  private String locality;
  private String georeferencedDate;
  private GadmFeatures gadm;
  private String footprintWKT;
  private String biome;
  private Double distanceFromCentroidInMeters;
  private List<String> higherGeography;
  private List<String> georeferencedBy;
  @lombok.Builder.Default private IssueRecord issues = IssueRecord.newBuilder().build();
}
