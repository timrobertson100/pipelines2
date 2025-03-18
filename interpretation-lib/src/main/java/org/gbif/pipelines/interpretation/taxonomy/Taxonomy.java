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
package org.gbif.pipelines.interpretation.taxonomy;

import java.util.concurrent.TimeUnit;

import lombok.SneakyThrows;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Taxonomy {

  private WebService service;

  public Taxonomy(String baseUrl, int poolSize) {
    service =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                new OkHttpClient.Builder()
                    .connectionPool(new ConnectionPool(poolSize, 1L, TimeUnit.MINUTES))
                    .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WebService.class);
  }

  @SneakyThrows
  public MatchResponse.Usage match(
      Integer usageKey,
      String kingdom,
      String phylum,
      String clazz,
      String order,
      String family,
      String genus,
      String scientificName,
      String genericName,
      String specificEpithet,
      String infraspecificEpithet,
      String scientificNameAuthorship,
      String rank,
      boolean verbose,
      boolean strict) {
    Call<MatchResponse> call =
        service.speciesMatch(
            usageKey,
            kingdom,
            phylum,
            clazz,
            order,
            family,
            genus,
            scientificName,
            genericName,
            specificEpithet,
            infraspecificEpithet,
            scientificNameAuthorship,
            rank,
            verbose,
            strict);
    retrofit2.Response<MatchResponse> response = call.execute();
    if (response.isSuccessful() && response.body() != null) {
      return response.body().getUsage() != null ? response.body().getUsage() : null;
    }
    return null;
  }

  interface WebService {
    @GET("/v1/species/match2")
    Call<MatchResponse> speciesMatch(
        @Query("usageKey") Integer usageKey,
        @Query("kingdom") String kingdom,
        @Query("phylum") String phylum,
        @Query("class") String clazz,
        @Query("order") String order,
        @Query("family") String family,
        @Query("genus") String genus,
        @Query("scientificName") String scientificName,
        @Query("genericName") String genericName,
        @Query("specificEpithet") String specificEpithet,
        @Query("infraspecificEpithet") String infraspecificEpithet,
        @Query("scientificNameAuthorship") String scientificNameAuthorship,
        @Query("rank") String rank,
        @Query("verbose") boolean verbose,
        @Query("strict") boolean strict);
  }
}
