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
package org.gbif.pipelines.parsers.location;

import org.gbif.kvs.geocode.LatLng;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.api.vocabulary.OccurrenceIssue.*;

/** Models a function that can be applied to a coordinates. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CoordinatesFunction {

  public static final UnaryOperator<LatLng> NEGATED_LAT_FN =
      latLng ->
          LatLng.create(
              -1d * latLng.getLatitude(), latLng.getLongitude(), latLng.getUncertaintyMeters());
  public static final UnaryOperator<LatLng> NEGATED_LNG_FN =
      latLng ->
          LatLng.create(
              latLng.getLatitude(), -1d * latLng.getLongitude(), latLng.getUncertaintyMeters());
  public static final UnaryOperator<LatLng> NEGATED_COORDS_FN =
      latLng ->
          LatLng.create(
              -1d * latLng.getLatitude(),
              -1d * latLng.getLongitude(),
              latLng.getUncertaintyMeters());
  public static final UnaryOperator<LatLng> SWAPPED_COORDS_FN =
      latLng ->
          LatLng.create(latLng.getLongitude(), latLng.getLatitude(), latLng.getUncertaintyMeters());

  public static Set<String> getIssueTypes(UnaryOperator<LatLng> transformation) {
    if (transformation == NEGATED_LAT_FN) {
      return Collections.singleton(PRESUMED_NEGATED_LATITUDE.name());
    }
    if (transformation == NEGATED_LNG_FN) {
      return Collections.singleton(PRESUMED_NEGATED_LONGITUDE.name());
    }
    if (transformation == NEGATED_COORDS_FN) {
      return new TreeSet<>(
          Arrays.asList(PRESUMED_NEGATED_LATITUDE.name(), PRESUMED_NEGATED_LONGITUDE.name()));
    }
    if (transformation == SWAPPED_COORDS_FN) {
      return Collections.singleton(PRESUMED_SWAPPED_COORDINATE.name());
    }

    return Collections.emptySet();
  }
}
