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
package org.gbif.pipelines.interpretation.spark;

import org.gbif.dwc.terms.DwcTerm;
import org.gbif.pipelines.models.ExtendedRecord;
import org.gbif.pipelines.models.LocationRecord;
import org.gbif.pipelines.models.MetadataRecord;
import org.gbif.pipelines.transform.LocationTransform;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.gbif.dwc.terms.DwcTerm.*;
import static org.gbif.pipelines.utils.ModelUtils.extractValue;

public class LocationInterpretation {

  /** Transforms the source records into the location records using the geocode service. */
  public static Dataset<LocationRecord> locationTransform(
      Config config, SparkSession spark, Dataset<ExtendedRecord> source) {
    LocationTransform locationTransform =
        LocationTransform.builder().geocodeApiUrl(config.getSpeciesMatchAPI()).build();

    // extract the location
    Dataset<RecordWithLocation> recordWithLocation =
        source.map(
            (MapFunction<ExtendedRecord, RecordWithLocation>)
                er -> {
                  Location location = Location.buildFrom(er);
                  return RecordWithLocation.builder()
                      .id(er.getId())
                      .coreId(er.getCoreId())
                      .parentId(extractValue(er, parentEventID))
                      .locationHash(location.hash())
                      .location(location)
                      .build();
                },
            Encoders.bean(RecordWithLocation.class));
    recordWithLocation.createOrReplaceTempView("record_with_location");

    // distinct the locations to lookup
    Dataset<Location> distinctLocations =
        spark
            .sql("SELECT DISTINCT location.* FROM record_with_location LIMIT 1000") // TODO! Limited
            // here just for
            // dev
            .repartition(config.getGeocodeParallelism())
            .as(Encoders.bean(Location.class));

    // lookup the distinct locations, and create a dictionary of the results
    Dataset<KeyedLocationRecord> keyedLocation =
        distinctLocations.map(
            (MapFunction<Location, KeyedLocationRecord>)
                location -> {

                  // HACK - the function takes ExtendedRecord, but we have a Location
                  ExtendedRecord er =
                      ExtendedRecord.newBuilder()
                          .setId("UNUSED_BUT_NECESSARY")
                          .setCoreTerms(location.toCoreTermsMap())
                          .build();
                  Optional<LocationRecord> converted =
                      locationTransform.convert(
                          er, MetadataRecord.newBuilder().build()); // TODO MetadataRecord
                  if (converted.isPresent())
                    return KeyedLocationRecord.builder()
                        .key(location.hash())
                        .locationRecord(converted.get())
                        .build();
                  else return null;
                },
            Encoders.bean(KeyedLocationRecord.class));
    keyedLocation.createOrReplaceTempView("key_location");

    // join the dictionary back to the source records
    Dataset<RecordWithLocationRecord> expanded =
        spark
            .sql(
                "SELECT id, coreId, parentId, locationRecord "
                    + "FROM record_with_location r "
                    + "  LEFT JOIN key_location l ON r.locationHash = l.key")
            .as(Encoders.bean(RecordWithLocationRecord.class));

    return expanded.map(
        (MapFunction<RecordWithLocationRecord, LocationRecord>)
            r -> {
              LocationRecord locationRecord = r.getLocationRecord();
              if (locationRecord != null) {
                locationRecord.setId(r.getId());
                locationRecord.setCoreId(r.getCoreId());
                locationRecord.setParentId(r.getParentId());
                return locationRecord;
              } else return null;
            },
        Encoders.bean(LocationRecord.class));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecordWithLocation {
    private String id;
    private String coreId;
    private String parentId;
    private String locationHash;
    private Location location;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class KeyedLocationRecord {
    private String key;
    private LocationRecord locationRecord;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RecordWithLocationRecord {
    private String id;
    private String coreId;
    private String parentId;
    private LocationRecord locationRecord;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Location {
    private String id;
    private String locationID;
    private String higherGeographyID;
    private String higherGeography;
    private String continent;
    private String waterBody;
    private String islandGroup;
    private String island;
    private String country;
    private String countryCode;
    private String stateProvince;
    private String county;
    private String municipality;
    private String locality;
    private String verbatimLocality;
    private String minimumElevationInMeters;
    private String maximumElevationInMeters;
    private String verbatimElevation;
    private String verticalDatum;
    private String minimumDepthInMeters;
    private String maximumDepthInMeters;
    private String verbatimDepth;
    private String minimumDistanceAboveSurfaceInMeters;
    private String maximumDistanceAboveSurfaceInMeters;
    private String locationAccordingTo;
    private String locationRemarks;
    private String decimalLatitude;
    private String decimalLongitude;
    private String geodeticDatum;
    private String coordinateUncertaintyInMeters;
    private String coordinatePrecision;
    private String pointRadiusSpatialFit;
    private String verbatimCoordinates;
    private String verbatimLatitude;
    private String verbatimLongitude;
    private String verbatimCoordinateSystem;
    private String verbatimSRS;
    private String footprintWKT;
    private String footprintSRS;
    private String footprintSpatialFit;
    private String georeferencedBy;
    private String georeferencedDate;
    private String georeferenceProtocol;
    private String georeferenceSources;
    private String georeferenceRemarks;

    static Location buildFrom(ExtendedRecord er) {
      Location.LocationBuilder builder = Location.builder();

      Arrays.stream(DwcTerm.values())
          .filter(t -> GROUP_LOCATION.equals(t.getGroup()) && !t.isClass())
          .forEach(
              term -> {
                String fieldName = term.simpleName(); // e.g., "country"
                String value =
                    er.getCoreTerms()
                        .get(term.qualifiedName()); // or however the ER provides values

                if (value != null) {
                  try {
                    Method setter = builder.getClass().getMethod(fieldName, String.class);
                    setter.invoke(builder, value);
                  } catch (NoSuchMethodException e) {
                    System.err.println("No setter for: " + fieldName);
                  } catch (Exception e) {
                    e.printStackTrace();
                  }
                }
              });

      return builder.build();
    }

    String hash() {
      return String.join(
          "|",
          id,
          locationID,
          higherGeographyID,
          higherGeography,
          continent,
          waterBody,
          islandGroup,
          island,
          country,
          countryCode,
          stateProvince,
          county,
          municipality,
          locality,
          verbatimLocality,
          minimumElevationInMeters,
          maximumElevationInMeters,
          verbatimElevation,
          verticalDatum,
          minimumDepthInMeters,
          maximumDepthInMeters,
          verbatimDepth,
          minimumDistanceAboveSurfaceInMeters,
          maximumDistanceAboveSurfaceInMeters,
          maximumDistanceAboveSurfaceInMeters,
          locationAccordingTo,
          locationRemarks,
          decimalLatitude,
          decimalLongitude,
          geodeticDatum,
          coordinateUncertaintyInMeters,
          coordinatePrecision,
          pointRadiusSpatialFit,
          verbatimCoordinates,
          verbatimLatitude,
          verbatimLongitude,
          verbatimCoordinateSystem,
          verbatimSRS,
          footprintWKT,
          footprintSRS,
          footprintSpatialFit,
          georeferencedBy,
          georeferencedDate,
          georeferenceProtocol,
          georeferenceSources,
          georeferenceRemarks);
    }

    public Map<String, String> toCoreTermsMap() {
      Map<String, String> coreTerms = new HashMap<>();

      if (getHigherGeographyID() != null)
        coreTerms.put(DwcTerm.higherGeographyID.qualifiedName(), getHigherGeographyID());
      if (getHigherGeography() != null)
        coreTerms.put(DwcTerm.higherGeography.qualifiedName(), getHigherGeography());
      if (getContinent() != null) coreTerms.put(DwcTerm.continent.qualifiedName(), getContinent());
      if (getWaterBody() != null) coreTerms.put(DwcTerm.waterBody.qualifiedName(), getWaterBody());
      if (getIslandGroup() != null)
        coreTerms.put(DwcTerm.islandGroup.qualifiedName(), getIslandGroup());
      if (getIsland() != null) coreTerms.put(DwcTerm.island.qualifiedName(), getIsland());
      if (getCountry() != null) coreTerms.put(DwcTerm.country.qualifiedName(), getCountry());
      if (getCountryCode() != null)
        coreTerms.put(DwcTerm.countryCode.qualifiedName(), getCountryCode());
      if (getStateProvince() != null)
        coreTerms.put(DwcTerm.stateProvince.qualifiedName(), getStateProvince());
      if (getCounty() != null) coreTerms.put(DwcTerm.county.qualifiedName(), getCounty());
      if (getMunicipality() != null)
        coreTerms.put(DwcTerm.municipality.qualifiedName(), getMunicipality());
      if (getLocality() != null) coreTerms.put(DwcTerm.locality.qualifiedName(), getLocality());
      if (getVerbatimLocality() != null)
        coreTerms.put(DwcTerm.verbatimLocality.qualifiedName(), getVerbatimLocality());
      if (getMinimumElevationInMeters() != null)
        coreTerms.put(
            DwcTerm.minimumElevationInMeters.qualifiedName(), getMinimumElevationInMeters());
      if (getMaximumElevationInMeters() != null)
        coreTerms.put(
            DwcTerm.maximumElevationInMeters.qualifiedName(), getMaximumElevationInMeters());
      if (getVerbatimElevation() != null)
        coreTerms.put(DwcTerm.verbatimElevation.qualifiedName(), getVerbatimElevation());
      if (getVerticalDatum() != null)
        coreTerms.put(DwcTerm.verticalDatum.qualifiedName(), getVerticalDatum());
      if (getMinimumDepthInMeters() != null)
        coreTerms.put(DwcTerm.minimumDepthInMeters.qualifiedName(), getMinimumDepthInMeters());
      if (getMaximumDepthInMeters() != null)
        coreTerms.put(DwcTerm.maximumDepthInMeters.qualifiedName(), getMaximumDepthInMeters());
      if (getVerbatimDepth() != null)
        coreTerms.put(DwcTerm.verbatimDepth.qualifiedName(), getVerbatimDepth());
      if (getMinimumDistanceAboveSurfaceInMeters() != null)
        coreTerms.put(
            DwcTerm.minimumDistanceAboveSurfaceInMeters.qualifiedName(),
            getMinimumDistanceAboveSurfaceInMeters());
      if (getMaximumDistanceAboveSurfaceInMeters() != null)
        coreTerms.put(
            DwcTerm.maximumDistanceAboveSurfaceInMeters.qualifiedName(),
            getMaximumDistanceAboveSurfaceInMeters());
      if (getLocationAccordingTo() != null)
        coreTerms.put(DwcTerm.locationAccordingTo.qualifiedName(), getLocationAccordingTo());
      if (getLocationRemarks() != null)
        coreTerms.put(DwcTerm.locationRemarks.qualifiedName(), getLocationRemarks());
      if (getDecimalLatitude() != null)
        coreTerms.put(DwcTerm.decimalLatitude.qualifiedName(), getDecimalLatitude());
      if (getDecimalLongitude() != null)
        coreTerms.put(DwcTerm.decimalLongitude.qualifiedName(), getDecimalLongitude());
      if (getGeodeticDatum() != null)
        coreTerms.put(DwcTerm.geodeticDatum.qualifiedName(), getGeodeticDatum());
      if (getCoordinateUncertaintyInMeters() != null)
        coreTerms.put(
            DwcTerm.coordinateUncertaintyInMeters.qualifiedName(),
            getCoordinateUncertaintyInMeters());
      if (getCoordinatePrecision() != null)
        coreTerms.put(DwcTerm.coordinatePrecision.qualifiedName(), getCoordinatePrecision());
      if (getPointRadiusSpatialFit() != null)
        coreTerms.put(DwcTerm.pointRadiusSpatialFit.qualifiedName(), getPointRadiusSpatialFit());
      if (getVerbatimCoordinates() != null)
        coreTerms.put(DwcTerm.verbatimCoordinates.qualifiedName(), getVerbatimCoordinates());
      if (getVerbatimLatitude() != null)
        coreTerms.put(DwcTerm.verbatimLatitude.qualifiedName(), getVerbatimLatitude());
      if (getVerbatimLongitude() != null)
        coreTerms.put(DwcTerm.verbatimLongitude.qualifiedName(), getVerbatimLongitude());
      if (getVerbatimCoordinateSystem() != null)
        coreTerms.put(
            DwcTerm.verbatimCoordinateSystem.qualifiedName(), getVerbatimCoordinateSystem());
      if (getVerbatimSRS() != null)
        coreTerms.put(DwcTerm.verbatimSRS.qualifiedName(), getVerbatimSRS());
      if (getFootprintWKT() != null)
        coreTerms.put(DwcTerm.footprintWKT.qualifiedName(), getFootprintWKT());
      if (getFootprintSRS() != null)
        coreTerms.put(DwcTerm.footprintSRS.qualifiedName(), getFootprintSRS());
      if (getFootprintSpatialFit() != null)
        coreTerms.put(DwcTerm.footprintSpatialFit.qualifiedName(), getFootprintSpatialFit());
      if (getGeoreferencedBy() != null)
        coreTerms.put(DwcTerm.georeferencedBy.qualifiedName(), getGeoreferencedBy());
      if (getGeoreferencedDate() != null)
        coreTerms.put(DwcTerm.georeferencedDate.qualifiedName(), getGeoreferencedDate());
      if (getGeoreferenceProtocol() != null)
        coreTerms.put(DwcTerm.georeferenceProtocol.qualifiedName(), getGeoreferenceProtocol());
      if (getGeoreferenceSources() != null)
        coreTerms.put(DwcTerm.georeferenceSources.qualifiedName(), getGeoreferenceSources());
      if (getGeoreferenceRemarks() != null)
        coreTerms.put(DwcTerm.georeferenceRemarks.qualifiedName(), getGeoreferenceRemarks());

      return coreTerms;
    }
  }
}
