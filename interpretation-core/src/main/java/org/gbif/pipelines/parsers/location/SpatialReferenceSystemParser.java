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

import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.geospatial.DatumParser;

import org.geotools.factory.BasicFactories;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.DatumAuthorityFactory;
import org.opengis.referencing.datum.GeodeticDatum;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpatialReferenceSystemParser {

  private static final DatumParser PARSER = DatumParser.getInstance();
  private static DatumAuthorityFactory datumFactory;
  private static Object LOCK = new Object();

  private static DatumAuthorityFactory getDatumFactory() {
    if (datumFactory == null) {
      synchronized (LOCK) {
        if (datumFactory == null) {
          try {
            System.out.println(BasicFactories.getDefault());
            System.out.println(BasicFactories.getDefault().getDatumAuthorityFactory());
            datumFactory = BasicFactories.getDefault().getDatumAuthorityFactory();
          } catch (FactoryRegistryException e) {
            e.printStackTrace();
            log.error("Failed to create geotools datum factory", e);
            throw e;
          }
        }
      }
    }
    return datumFactory;
  }

  static {
    try {
      // System.out.println(BasicFactories.getDefault());
      // System.out.println(BasicFactories.getDefault().getDatumAuthorityFactory());
      // datumFactory = BasicFactories.getDefault().getDatumAuthorityFactory();
    } catch (FactoryRegistryException e) {
      log.error("Failed to create geotools datum factory", e);
      throw e;
    }
  }

  /**
   * Parses the given datum or SRS code and constructs a full 2D geographic reference system.
   *
   * @return the parsed CRS or null if it can't be interpreted
   */
  public static CoordinateReferenceSystem parseCRS(String datum) {
    CoordinateReferenceSystem crs = null;
    ParseResult<Integer> epsgCode = PARSER.parse(datum);
    if (epsgCode.isSuccessful()) {
      final String code = "EPSG:" + epsgCode.getPayload();

      // first try to create a full fledged CRS from the given code
      try {
        crs = CRS.decode(code);

      } catch (FactoryException e) {
        // that didn't work, maybe it is *just* a datum
        try {
          GeodeticDatum dat = getDatumFactory().createGeodeticDatum(code);
          crs = new DefaultGeographicCRS(dat, DefaultEllipsoidalCS.GEODETIC_2D);

        } catch (FactoryException e1) {
          // also not a datum, no further ideas, log error
          // swallow anything and return null instead
          log.info("No CRS or DATUM for given datum code >>{}<<: {}", datum, e1.getMessage());
        }
      }
    }
    return crs;
  }
}
