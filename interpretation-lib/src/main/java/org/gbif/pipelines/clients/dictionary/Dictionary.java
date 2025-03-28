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
package org.gbif.pipelines.clients.dictionary;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import lombok.Getter;
import lombok.SneakyThrows;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class Dictionary {

  private WebService service;
  private LoadingCache<String, List<String>> cache;

  public Dictionary(String baseUrl, int poolSize) {
    service =
        new Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(
                new OkHttpClient.Builder()
                    .connectionPool(new ConnectionPool(poolSize, 1L, TimeUnit.MINUTES))
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .readTimeout(1, TimeUnit.MINUTES)
                    .writeTimeout(1, TimeUnit.MINUTES)
                    .build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WebService.class);

    cache =
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            // .expireAfterWrite(1, TimeUnit.HOURS)
            .build(
                new CacheLoader<>() {
                  @Override
                  public List<String> load(String vocabAndQuery) throws IOException {
                    String[] parts = vocabAndQuery.split("\\|");
                    Call<List<Concept>> call = service.lookup(parts[0], parts[1]);
                    retrofit2.Response<List<Concept>> response = call.execute();
                    if (response.isSuccessful() && response.body() != null) {
                      return response.body().stream()
                          .map(c -> c.conceptName)
                          .collect(Collectors.toList());
                    } else if (!response.isSuccessful()) {
                      return new ArrayList<>(); // TODO - what do we want to do?
                    } else {
                      return new ArrayList<>();
                    }
                  }
                });
  }

  @SneakyThrows
  public List<String> lookup(String vocabulary, String q) {
    return cache.get(
        vocabulary + "|" + q); // TODO - need an object to contain the key. Need to support arrays
  }

  interface WebService {
    @GET("/v1/vocabularies/{vocabulary}/concepts/lookup")
    Call<List<Concept>> lookup(@Path("vocabulary") String vocabulary, @Query("q") String q);
  }

  @Getter
  class Concept {
    private String conceptName;
    private String conceptLink;
    private String matchedHiddenLabel;
  }
}
