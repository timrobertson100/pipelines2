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
package org.gbif.pipelines.clients.taxonomy;

import java.util.List;

import com.google.gson.annotations.SerializedName;

import lombok.Getter;

@Getter
public class MatchResponse {

  @SerializedName("synonym")
  private boolean synonym;

  @SerializedName("usage")
  private Usage usage;

  @SerializedName("classification")
  private List<Classification> classification;

  @SerializedName("diagnostics")
  private Diagnostics diagnostics;

  @SerializedName("additionalStatus")
  private List<AdditionalStatus> additionalStatus;

  @Getter
  public static class Usage {
    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("canonicalName")
    private String canonicalName;

    @SerializedName("authorship")
    private String authorship;

    @SerializedName("rank")
    private String rank;

    @SerializedName("code")
    private String code;

    @SerializedName("genus")
    private String genus;

    @SerializedName("specificEpithet")
    private String specificEpithet;

    @SerializedName("candidatus")
    private boolean candidatus;

    @SerializedName("type")
    private String type;

    @SerializedName("extinct")
    private boolean extinct;

    @SerializedName("basionymAuthorship")
    private BasionymAuthorship basionymAuthorship;

    @SerializedName("combinationAuthorship")
    private CombinationAuthorship combinationAuthorship;

    @SerializedName("doubtful")
    private boolean doubtful;

    @SerializedName("manuscript")
    private boolean manuscript;

    @SerializedName("warnings")
    private List<String> warnings;

    @SerializedName("abbreviated")
    private boolean abbreviated;

    @SerializedName("autonym")
    private boolean autonym;

    @SerializedName("binomial")
    private boolean binomial;

    @SerializedName("trinomial")
    private boolean trinomial;

    @SerializedName("incomplete")
    private boolean incomplete;

    @SerializedName("indetermined")
    private boolean indetermined;

    @SerializedName("phraseName")
    private boolean phraseName;

    @Getter
    public static class BasionymAuthorship {
      @SerializedName("authors")
      private List<String> authors;

      @SerializedName("year")
      private String year;
    }

    @Getter
    public static class CombinationAuthorship {
      @SerializedName("authors")
      private List<String> authors;

      @SerializedName("year")
      private String year;
    }
  }

  @Getter
  public static class Classification {
    @SerializedName("key")
    private String key;

    @SerializedName("name")
    private String name;

    @SerializedName("rank")
    private String rank;
  }

  @Getter
  public static class Diagnostics {
    @SerializedName("matchType")
    private String matchType;

    @SerializedName("confidence")
    private int confidence;

    @SerializedName("status")
    private String status;

    @SerializedName("timeTaken")
    private int timeTaken;

    @SerializedName("note")
    private String note;

    @SerializedName("alternatives")
    private List<Alternative> alternatives;

    @Getter
    public static class Alternative {
      @SerializedName("synonym")
      private boolean synonym;

      @SerializedName("usage")
      private Usage usage;

      @SerializedName("classification")
      private List<Classification> classification;

      @SerializedName("diagnostics")
      private Diagnostics diagnostics;
    }
  }

  @Getter
  public static class AdditionalStatus {
    @SerializedName("datasetKey")
    private String datasetKey;

    @SerializedName("datasetAlias")
    private String datasetAlias;

    @SerializedName("gbifKey")
    private String gbifKey;

    @SerializedName("status")
    private String status;

    @SerializedName("statusCode")
    private String statusCode;

    @SerializedName("sourceId")
    private String sourceId;
  }
}
