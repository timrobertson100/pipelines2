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
package org.gbif.pipelines.parsers.common;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import lombok.Builder;
import lombok.Getter;

/**
 * Models a parsed field.
 *
 * <p>A field can be as simple as a single {@link org.gbif.dwc.terms.DwcTerm} or a wrapper with
 * several fields inside.
 */
@Builder
@Getter
public class ParsedField<T> {

  private final T result;

  @Builder.Default private final Set<String> issues = new TreeSet<>();

  private final boolean successful;

  public static <S> ParsedField<S> fail(S result, Set<String> issues) {
    return ParsedField.<S>builder().result(result).issues(issues).build();
  }

  public static <S> ParsedField<S> fail(String issue) {
    return ParsedField.<S>builder().issues(Collections.singleton(issue)).build();
  }

  public static <S> ParsedField<S> fail(Set<String> issues) {
    return ParsedField.<S>builder().issues(issues).build();
  }

  public static <S> ParsedField<S> fail() {
    return ParsedField.<S>builder().build();
  }

  public static <S> ParsedField<S> success(S result, Set<String> issues) {
    return ParsedField.<S>builder().successful(true).result(result).issues(issues).build();
  }

  public static <S> ParsedField<S> success(S result) {
    return ParsedField.<S>builder().successful(true).result(result).build();
  }
}
