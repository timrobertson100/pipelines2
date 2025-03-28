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
package org.gbif.pipelines.interpretation.spark.udf;

import org.gbif.pipelines.clients.dictionary.Dictionary;
import org.gbif.pipelines.interpretation.spark.Config;

import java.util.List;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.api.java.UDF2;
import org.apache.spark.sql.types.DataTypes;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DictionaryUDF implements UDF2<String, String, String> {

  private transient Dictionary dictionary;
  private final Object lock = new Object[0];
  private final String baseUrl;
  private final int poolSize;

  public static void register(SparkSession spark, String name, Config conf) {
    spark
        .udf()
        .register(
            name,
            new DictionaryUDF(
                conf.getDictionary().getBaseUrl(), conf.getDictionary().getPoolSize()),
            DataTypes.StringType);
  }

  private Dictionary dictionary() {
    if (dictionary == null)
      synchronized (lock) {
        dictionary = new Dictionary(baseUrl, poolSize);
      }
    return dictionary;
  }

  @Override
  public String call(String vocabulary, String q) {
    List<String> options = dictionary().lookup(vocabulary, q);
    return options.size() == 1 ? options.get(0) : null;
  }
}
