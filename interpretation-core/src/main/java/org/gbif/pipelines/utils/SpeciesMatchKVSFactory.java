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
package org.gbif.pipelines.utils;

import org.gbif.kvs.KeyValueStore;
import org.gbif.kvs.species.IdMappingConfiguration;
import org.gbif.kvs.species.Identification;
import org.gbif.kvs.species.NameUsageMatchKVStoreFactory;
import org.gbif.rest.client.configuration.ChecklistbankClientsConfiguration;
import org.gbif.rest.client.configuration.ClientConfiguration;
import org.gbif.rest.client.species.NameUsageMatch;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Provides the {@link KeyValueStore} as a singleton per JVM. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpeciesMatchKVSFactory {
  private static Object LOCK = new Object();
  private static KeyValueStore<Identification, NameUsageMatch> kvStore;

  public static KeyValueStore<Identification, NameUsageMatch> getKvStore(String api) {
    if (kvStore == null) {
      synchronized (LOCK) {
        if (kvStore == null) {
          ChecklistbankClientsConfiguration clientConfiguration =
              ChecklistbankClientsConfiguration.builder()
                  .nameUsageClientConfiguration(
                      ClientConfiguration.builder()
                          .withBaseApiUrl(api)
                          .withFileCacheMaxSizeMb(100L) // 100MB
                          .withTimeOut(10L) // secs
                          .build())
                  .checklistbankClientConfiguration(
                      ClientConfiguration.builder()
                          .withBaseApiUrl(api)
                          .withFileCacheMaxSizeMb(100L) // 100MB
                          .withTimeOut(10L) // secs
                          .build())
                  .build();

          IdMappingConfiguration idMapping = IdMappingConfiguration.builder().build(); // TODO

          kvStore =
              NameUsageMatchKVStoreFactory.nameUsageMatchKVStore(clientConfiguration, idMapping);
        }
      }
    }

    return kvStore;
  }
}
