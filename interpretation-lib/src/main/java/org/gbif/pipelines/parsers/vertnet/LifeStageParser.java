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
package org.gbif.pipelines.parsers.vertnet;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Java version of
 * https://github.com/VertNet/post-harvest-processor/blob/master/lib/trait_parsers/life_stage_parser.py
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LifeStageParser {

  private static final Pattern EXCLUDE_PATTERN = Pattern.compile("determin");
  private static final Pattern LIFE_STAGE_KEY_VALUE_DELIMITED =
      Pattern.compile(
          "\\b(?<key>(?:life\\s*stage(?:\\s*remarks)?|age(?:\\s*class)?))\\W+(?<value>[\\w?./\\-]+(?:\\s+[\\w?./\\-]+){0,4})\\s*(?:[:;,\"]|$)");
  private static final Pattern LIFE_STAGE_KEY_VALUE_UNDELIMITED =
      Pattern.compile(
          "\\b(?<key>life\\s*stage(?:\\s*remarks)?|age\\s*class|age\\s*in\\s*(?:hour|day)s?|age)\\W+(?<value>[\\w?./\\-]+(?:\\s+(?:year|recorded))?)");
  private static final Pattern LIFE_STAGE_NO_KEYWORD =
      Pattern.compile("(?<value>(?:after\\s+)?(?:first|second|third|fourth|hatching)\\s+year)");
  private static final Pattern LIFE_STAGE_YOLK_SAC = Pattern.compile("(?<value>(?:yolk)\\s+sac)");
  private static final Pattern LIFE_STAGE_UNKEYED =
      Pattern.compile(
          "\\b(?<value>(?:larves?|larvae?|larvals?|imagos?|neonates?|hatchlings?|hatched?|fry?|metamorphs?|premetamorphs"
              + "|tadpoles?|têtard?|young-of-the-year?|leptocephales?|leptocephalus?|immatures?|imms?|jeunes?"
              + "|young?|ygs?|fleglings?|fledgelings?|chicks?|nestlings?|juveniles?|juvéniles?|juvs?|subadults?"
              + "|subadultes?|subads?|sub-adults?|yearlings?|matures?|adults?|adulte?|ads?)(?:\\s*\\?)?)\\b");

  private static final List<Pattern> PATTERNS =
      Arrays.asList(
          LIFE_STAGE_KEY_VALUE_DELIMITED,
          LIFE_STAGE_KEY_VALUE_UNDELIMITED,
          LIFE_STAGE_NO_KEYWORD,
          LIFE_STAGE_YOLK_SAC,
          LIFE_STAGE_UNKEYED);

  public static Optional<String> parse(String source) {
    if (source == null || source.isEmpty()) {
      return Optional.empty();
    }

    try {
      Matcher exclude = EXCLUDE_PATTERN.matcher(source.toLowerCase());
      if (exclude.find()) {
        return Optional.empty();
      }
      for (Pattern p : PATTERNS) {
        Matcher matcher = p.matcher(source.toLowerCase());
        if (matcher.find()) {
          return Optional.ofNullable(matcher.group("value"));
        }
      }
    } catch (RuntimeException ex) {
      return Optional.empty();
    }

    return Optional.empty();
  }
}
