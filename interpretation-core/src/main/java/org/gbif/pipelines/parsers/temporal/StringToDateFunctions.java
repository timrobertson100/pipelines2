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
package org.gbif.pipelines.parsers.temporal;

import org.gbif.common.parsers.date.TemporalAccessorUtils;

import java.time.*;
import java.time.chrono.IsoChronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.TemporalAccessor;
import java.util.function.Function;

import com.google.common.base.Strings;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import static java.time.temporal.ChronoField.*;

/*
 * Note java.util.Date should be avoided, as it does not handle dates before 1582 without additional effort.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StringToDateFunctions {

  // Supported Date formats
  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern(
          "[yyyy[-MM[-dd['T'HH:mm[:ss[.SSSSSSSSS][.SSSSSSSS][.SSSSSSS][.SSSSSS][.SSSSS][.SSSS][.SSS][.SS][.S]]]]][ ][XXXXX][XXXX][XXX][XX][X]]");

  // DateTimeFormatter.ISO_LOCAL_TIME but limited to milliseconds and with those milliseconds
  // printed
  public static final DateTimeFormatter ISO_LOCAL_TIME_MILLISECONDS =
      new DateTimeFormatterBuilder()
          .appendValue(HOUR_OF_DAY, 2)
          .appendLiteral(':')
          .appendValue(MINUTE_OF_HOUR, 2)
          .optionalStart()
          .appendLiteral(':')
          .appendValue(SECOND_OF_MINUTE, 2)
          .optionalStart()
          .appendFraction(NANO_OF_SECOND, 3, 3, true)
          .toFormatter()
          .withResolverStyle(ResolverStyle.STRICT);

  // DateTimeFormatter.ISO_LOCAL_DATE_TIME but limited to milliseconds
  public static final DateTimeFormatter ISO_LOCAL_DATE_TIME_MILLISECONDS =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .append(DateTimeFormatter.ISO_LOCAL_DATE)
          .appendLiteral('T')
          .append(ISO_LOCAL_TIME_MILLISECONDS)
          .toFormatter()
          .withResolverStyle(ResolverStyle.STRICT)
          .withChronology(IsoChronology.INSTANCE);

  // DateTimeFormatter.ISO_OFFSET_DATE_TIME but limited to milliseconds
  public static final DateTimeFormatter ISO_OFFSET_DATE_TIME_MILLISECONDS =
      new DateTimeFormatterBuilder()
          .parseCaseInsensitive()
          .append(ISO_LOCAL_DATE_TIME_MILLISECONDS)
          .appendOffsetId()
          .toFormatter()
          .withResolverStyle(ResolverStyle.STRICT)
          .withChronology(IsoChronology.INSTANCE);

  public static Function<TemporalAccessor, String> getTemporalToStringFn() {
    return temporalAccessor -> {
      if (temporalAccessor instanceof ZonedDateTime) {
        return ((ZonedDateTime) temporalAccessor).format(ISO_OFFSET_DATE_TIME_MILLISECONDS);
      } else if (temporalAccessor instanceof LocalDateTime) {
        return ((LocalDateTime) temporalAccessor).format(ISO_LOCAL_DATE_TIME_MILLISECONDS);
      } else if (temporalAccessor instanceof LocalDate) {
        return ((LocalDate) temporalAccessor).format(DateTimeFormatter.ISO_LOCAL_DATE);
      } else {
        return temporalAccessor.toString();
      }
    };
  }

  public static Function<String, TemporalAccessor> getStringToTemporalAccessor() {
    return dateAsString -> {
      if (Strings.isNullOrEmpty(dateAsString)) {
        return null;
      }

      if (dateAsString.startsWith("0000")) {
        dateAsString = dateAsString.replaceFirst("0000", "1970");
      }

      try {
        // parse string
        return FORMATTER.parseBest(
            dateAsString,
            ZonedDateTime::from,
            LocalDateTime::from,
            LocalDate::from,
            YearMonth::from,
            Year::from);

      } catch (Exception ex) {
        return null;
      }
    };
  }

  public static Function<String, Long> getStringToEarliestEpochSeconds(boolean ignoreOffset) {
    return dateAsString -> {
      if (Strings.isNullOrEmpty(dateAsString)) {
        return null;
      }

      TemporalAccessor ta = getStringToTemporalAccessor().apply(dateAsString);
      if (ta == null) {
        return null;
      }
      return TemporalAccessorUtils.toEarliestLocalDateTime(ta, ignoreOffset)
          .toEpochSecond(ZoneOffset.UTC);
    };
  }

  public static Function<String, Long> getStringToLatestEpochSeconds(boolean ignoreOffset) {
    return dateAsString -> {
      if (Strings.isNullOrEmpty(dateAsString)) {
        return null;
      }

      TemporalAccessor ta = getStringToTemporalAccessor().apply(dateAsString);
      if (ta == null) {
        return null;
      }
      return TemporalAccessorUtils.toLatestLocalDateTime(ta, ignoreOffset)
          .toEpochSecond(ZoneOffset.UTC);
    };
  }
}
