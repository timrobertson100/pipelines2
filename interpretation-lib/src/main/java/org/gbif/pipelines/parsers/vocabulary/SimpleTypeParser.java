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
package org.gbif.pipelines.parsers.vocabulary;

import org.gbif.common.parsers.NumberParser;
import org.gbif.dwc.terms.DwcTerm;
import org.gbif.pipelines.models.ExtendedRecord;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static org.gbif.pipelines.utils.ModelUtils.extractNullAwareOptValue;
import static org.gbif.pipelines.utils.ModelUtils.extractNullAwareValue;

/** Utility class that parses basic data types. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SimpleTypeParser {

  private static final Pattern INT_PATTERN = Pattern.compile("(^-?\\d{1,10}$)");
  private static final Pattern INT_POSITIVE_PATTERN = Pattern.compile("(^\\d{1,10}$)");

  /** Parses an integer value and consumes its response (if any). */
  public static void parseInt(
      ExtendedRecord er, DwcTerm term, Consumer<Optional<Integer>> consumer) {
    extractNullAwareOptValue(er, term)
        .ifPresent(
            termValue -> {
              boolean matches = INT_PATTERN.matcher(termValue).matches();
              Optional<Integer> v =
                  matches
                      ? Optional.ofNullable(NumberParser.parseInteger(termValue))
                      : Optional.empty();
              consumer.accept(v);
            });
  }

  /** Parses a positive integer value and consumes its response (if any). */
  public static void parsePositiveInt(
      ExtendedRecord er, DwcTerm term, Consumer<Optional<Integer>> consumer) {
    extractNullAwareOptValue(er, term)
        .ifPresent(termValue -> consumer.accept(parsePositiveIntOpt(termValue)));
  }

  /** Parses a positive integer value and consumes its response (if any). */
  public static Optional<Integer> parsePositiveIntOpt(String value) {
    if (value == null) {
      return Optional.empty();
    }
    boolean matches = INT_POSITIVE_PATTERN.matcher(value).matches();
    return matches ? Optional.ofNullable(NumberParser.parseInteger(value)) : Optional.empty();
  }

  /** Parses a double value and consumes its response (if any). */
  public static void parseDouble(
      ExtendedRecord er, DwcTerm term, Consumer<Optional<Double>> consumer) {
    parseDouble(extractNullAwareValue(er, term), consumer);
  }

  /** Parses a double value and consumes its response (if any). */
  public static void parseDouble(String value, Consumer<Optional<Double>> consumer) {
    Optional.ofNullable(value)
        .ifPresent(
            termValue -> consumer.accept(Optional.ofNullable(NumberParser.parseDouble(termValue))));
  }
}
