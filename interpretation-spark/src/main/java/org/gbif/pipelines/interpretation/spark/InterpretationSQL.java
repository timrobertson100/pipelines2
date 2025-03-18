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

interface InterpretationSQL {
  /** Expose fields to be interpreted in a flat view to simplify SQL and improve performance. */
  String SOURCE_VIEW =
      """
        CREATE OR REPLACE TEMPORARY VIEW source AS (
          SELECT /*+ REBALANCE */
            id,
            coreTerms['http://rs.tdwg.org/dwc/terms/geoTime'] AS geoTime,
            coreTerms['http://rs.tdwg.org/dwc/terms/typeStatus'] AS typeStatus,
            coreTerms['http://rs.tdwg.org/dwc/terms/geodeticDatum'] AS geodeticDatum,
            coreTerms['http://rs.tdwg.org/dwc/terms/sex'] AS sex,
            coreTerms['http://rs.tdwg.org/dwc/terms/month'] AS month,
            coreTerms['http://rs.tdwg.org/dwc/terms/pathway'] AS pathway,
            coreTerms['http://rs.tdwg.org/dwc/terms/degreeOfEstablishment'] AS degreeOfEstablishment,
            coreTerms['http://rs.tdwg.org/dwc/terms/establishmentMeans'] AS establishmentMeans,
            coreTerms['http://rs.tdwg.org/dwc/terms/occurrenceStatus'] AS occurrenceStatus,
            coreTerms['http://rs.tdwg.org/dwc/terms/lifeStage'] AS lifeStage,
            coreTerms['http://rs.tdwg.org/dwc/terms/usageKey'] AS usageKey,
            coreTerms['http://rs.tdwg.org/dwc/terms/kingdom'] AS kingdom,
            coreTerms['http://rs.tdwg.org/dwc/terms/phylum'] AS phylum,
            coreTerms['http://rs.tdwg.org/dwc/terms/class'] AS class,
            coreTerms['http://rs.tdwg.org/dwc/terms/order'] AS order_,
            coreTerms['http://rs.tdwg.org/dwc/terms/family'] AS family,
            coreTerms['http://rs.tdwg.org/dwc/terms/genus'] AS genus,
            coreTerms['http://rs.tdwg.org/dwc/terms/scientificName'] AS scientificName,
            coreTerms['http://rs.tdwg.org/dwc/terms/genericName'] AS genericName,
            coreTerms['http://rs.tdwg.org/dwc/terms/specificEpithet'] AS specificEpithet,
            coreTerms['http://rs.tdwg.org/dwc/terms/infraspecificEpithet'] AS infraspecificEpithet,
            coreTerms['http://rs.tdwg.org/dwc/terms/scientificNameAuthorship'] AS scientificNameAuthorship,
            coreTerms['http://rs.tdwg.org/dwc/terms/taxonRank'] AS taxonRank
          FROM verbatim
        )""";

  /**
   * SQL template for extracting distinct values, and lookup them up against the vocabulary server.
   */
  String DICT_VIEW_TEMPLATE =
      """
        CREATE OR REPLACE TEMPORARY VIEW dict_%1$s AS (
          SELECT orig, dictionaryLookup('%2$s', orig) AS interp
          FROM (SELECT DISTINCT %1$s AS orig FROM source)
        )""";

  /** Session local dictionaries created for all terms controlled by a vocabulary. */
  String[] DICTIONARIES = {
    String.format(DICT_VIEW_TEMPLATE, "geoTime", "GeoTime"),
    String.format(DICT_VIEW_TEMPLATE, "typeStatus", "TypeStatus"),
    String.format(DICT_VIEW_TEMPLATE, "geodeticDatum", "GeodeticDatum"),
    String.format(DICT_VIEW_TEMPLATE, "sex", "Sex"),
    String.format(DICT_VIEW_TEMPLATE, "taxonRank", "TaxonRank"),
    String.format(DICT_VIEW_TEMPLATE, "kingdom", "Kingdom"),
    String.format(DICT_VIEW_TEMPLATE, "month", "Month"),
    String.format(DICT_VIEW_TEMPLATE, "pathway", "Pathway"),
    String.format(DICT_VIEW_TEMPLATE, "degreeOfEstablishment", "DegreeOfEstablishment"),
    String.format(DICT_VIEW_TEMPLATE, "establishmentMeans", "EstablishmentMeans"),
    String.format(DICT_VIEW_TEMPLATE, "occurrenceStatus", "OccurrenceStatus"),
    String.format(DICT_VIEW_TEMPLATE, "lifeStage", "LifeStage")
  };

  /** Dictionary created for taxonomic organisation of the distinct identifications. */
  String TAXONOMY =
      """
          CREATE OR REPLACE TEMPORARY VIEW taxonomy AS (
            SELECT
              usageKey, kingdom, phylum, class, order_, family, genus, scientificName, genericName, specificEpithet,
              infraspecificEpithet, scientificNameAuthorship, taxonRank,
              nameMatch(
                usageKey, kingdom, phylum, class, order_, family, genus, scientificName, genericName, specificEpithet,
                infraspecificEpithet, scientificNameAuthorship, taxonRank) AS matchedTaxon
            FROM (
              SELECT /*+ REPARTITION(12) */ DISTINCT
                usageKey, kingdom, phylum, class, order_, family, genus, scientificName, genericName, specificEpithet,
                infraspecificEpithet, scientificNameAuthorship, taxonRank
              FROM source
            )
          )""";

  /** Basic parsing and interpretation using lookup dictionaries for single fields. */
  String PARSE_SOURCE =
      """
          CREATE OR REPLACE TEMPORARY VIEW parsed AS (
            SELECT
              /*+ REBALANCE */
              s.id,
              dict_geoTime.interp AS geoTime,
              dict_typeStatus.interp AS typeStatus,
              dict_geodeticDatum.interp AS geodeticDatum,
              dict_sex.interp AS sex,
              dict_taxonRank.interp AS taxonRank,
              dict_kingdom.interp AS kingdom,
              dict_month.interp AS month,
              dict_pathway.interp AS pathway,
              dict_degreeOfEstablishment.interp AS degreeOfEstablishment,
              dict_establishmentMeans.interp AS establishmentMeans,
              dict_occurrenceStatus.interp AS occurrenceStatus,
              dict_lifeStage.interp AS lifeStage,
              t.scientificName AS scientificName,
              t.matchedTaxon.taxonId AS gbif_taxonId,
              t.matchedTaxon.scientificName AS gbif_scientificName
            FROM
              source s
                LEFT JOIN dict_geoTime ON geoTime = dict_geoTime.orig
                LEFT JOIN dict_typeStatus ON typeStatus = dict_typeStatus.orig
                LEFT JOIN dict_geodeticDatum ON geodeticDatum = dict_geodeticDatum.orig
                LEFT JOIN dict_sex ON sex = dict_sex.orig
                LEFT JOIN dict_taxonRank ON taxonRank = dict_taxonRank.orig
                LEFT JOIN dict_kingdom ON kingdom = dict_kingdom.orig
                LEFT JOIN dict_month ON month = dict_month.orig
                LEFT JOIN dict_pathway ON pathway = dict_pathway.orig
                LEFT JOIN dict_degreeOfEstablishment ON degreeOfEstablishment = dict_degreeOfEstablishment.orig
                LEFT JOIN dict_establishmentMeans ON establishmentMeans = dict_establishmentMeans.orig
                LEFT JOIN dict_occurrenceStatus ON occurrenceStatus = dict_occurrenceStatus.orig
                LEFT JOIN dict_lifeStage ON lifeStage = dict_lifeStage.orig
                LEFT JOIN taxonomy t ON
                  concat_ws('|',
                    s.usageKey, s.kingdom, s.phylum, s.class, s.order_, s.family, s.genus, s.scientificName,
                    s.genericName, s.specificEpithet, s.infraspecificEpithet, s.scientificNameAuthorship, s.taxonRank)
                  =
                  concat_ws('|',
                    t.usageKey, t.kingdom, t.phylum, t.class, t.order_, t.family, t.genus, t.scientificName,
                    t.genericName, t.specificEpithet, t.infraspecificEpithet, t.scientificNameAuthorship, t.taxonRank)
          )""";
}
