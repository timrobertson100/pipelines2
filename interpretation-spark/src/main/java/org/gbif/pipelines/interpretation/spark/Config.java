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

import lombok.Value;

/**
 * TODO: Design config properly. For now, it's encapsulated to centralise values and simplify code.
 */
@Value
public class Config {
  Dictionary dictionary = new Dictionary();
  Taxonomy taxonomy = new Taxonomy();

  @Value
  public static class Dictionary {
    String baseUrl = "https://api.gbif.org/"; // vocabulary server providing the lookup
    int poolSize = 5; // http concurrency per executor; at most the number of cores per executor
  }

  @Value
  public static class Taxonomy {
    String baseUrl = "https://api.gbif.org/"; // species match service
    int poolSize = 20; // http concurrency per executor; at most the number of cores per executor
  }
}
