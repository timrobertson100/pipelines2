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

import org.gbif.kvs.KeyValueStore;
import org.gbif.kvs.geocode.LatLng;
import org.gbif.pipelines.models.LocationRecord;
import org.gbif.rest.client.geocode.GeocodeResponse;
import org.gbif.rest.client.geocode.Location;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CentroidCalculator {

  // The maximum distance to set, beyond this the value will be left null.
  // https://github.com/gbif/portal-feedback/issues/4232#issuecomment-1306884884
  public static final double MAXIMUM_DISTANCE_FROM_CENTROID_METRES = 5000;

  public static Optional<Double> calculateCentroidDistance(
      LocationRecord lr, KeyValueStore<LatLng, GeocodeResponse> kvStore) {
    Objects.requireNonNull(lr, "LocationRecord is required");
    Objects.requireNonNull(kvStore, "GeocodeService kvStore is required");

    // Take parsed values. Uncertainty isn't needed, but included anyway so we hit the cache.
    LatLng latLng =
        LatLng.create(
            lr.getDecimalLatitude(),
            lr.getDecimalLongitude(),
            lr.getCoordinateUncertaintyInMeters());
    // Use these to retrieve the centroid distances.
    // Check parameters
    Objects.requireNonNull(latLng);
    if (latLng.getLatitude() == null || latLng.getLongitude() == null) {
      throw new IllegalArgumentException("Empty coordinates");
    }

    return getDistanceToNearestCentroid(latLng, kvStore);
  }

  private static Optional<Double> getDistanceToNearestCentroid(
      LatLng latLng, KeyValueStore<LatLng, GeocodeResponse> geocodeKvStore) {
    if (latLng.isValid()) {
      GeocodeResponse geocodeResponse = geocodeKvStore.get(latLng);
      if (geocodeResponse != null && !geocodeResponse.getLocations().isEmpty()) {
        Optional<Double> centroidDistance =
            geocodeResponse.getLocations().stream()
                .filter(
                    l ->
                        "Centroids".equals(l.getType())
                            && l.getDistanceMeters() != null
                            && l.getDistanceMeters() <= MAXIMUM_DISTANCE_FROM_CENTROID_METRES)
                .sorted(Comparator.comparingDouble(Location::getDistanceMeters))
                .findFirst()
                .map(Location::getDistanceMeters);
        return centroidDistance;
      }
    }
    return Optional.empty();
  }
}
