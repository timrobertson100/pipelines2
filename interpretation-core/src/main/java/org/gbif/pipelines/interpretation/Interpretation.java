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
package org.gbif.pipelines.interpretation;

import java.util.Optional;
import java.util.function.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

/**
 * The class is designed to simplify interpretation process:
 *
 * <pre>
 *   1) Sets source data object {@link Interpretation#from}
 *   2) Sets target data object {@link Interpretation#to}
 *   3) Uses interpretation function {@link Handler#via}
 *   4) Consumes {@link Handler#consume} or get {@link Handler#getOfNullable} the result
 * </pre>
 *
 * <p>Example:
 *
 * <pre>{@code
 * Interpretation.from(context::element)
 *     .to(er -> TemporalRecord.newBuilder().setId(er.getId()).build())
 *     .via(TemporalInterpreter::interpretEventDate)
 *     .via(TemporalInterpreter::interpretDateIdentified)
 *     .via(TemporalInterpreter::interpretModifiedDate)
 *     .via(TemporalInterpreter::interpretDayOfYear)
 *     .consume(context::output);
 * }</pre>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Interpretation<S> {

  private final S source;

  /** @param source source data object */
  public static <S> Interpretation<S> from(S source) {
    return new Interpretation<>(source);
  }

  /** @param func Supplier produces source data object */
  public static <S> Interpretation<S> from(Supplier<S> func) {
    return new Interpretation<>(func.get());
  }

  /** @param target target data object */
  public <T> Condition<T> to(T target) {
    return new Condition<>(target);
  }

  /** @param func Function converts source data object to target data object */
  public <T> Condition<T> to(Function<S, T> func) {
    return new Condition<>(func.apply(source));
  }

  /** @param func Supplier produces target data object */
  public <T> Condition<T> to(Supplier<T> func) {
    return new Condition<>(func.get());
  }

  public class Condition<T> {

    private final T target;

    private Condition(T target) {
      this.target = target;
    }

    public Condition<T> when(Predicate<S> predicate) {
      return target != null && predicate.test(source) ? this : new Condition<>(null);
    }

    public Handler<T> via(BiConsumer<S, T> func) {
      return new Handler<>(target, null).via(func);
    }

    public Handler<T> via(Consumer<T> func) {
      return new Handler<>(target, null).via(func);
    }
  }

  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public class Handler<T> {

    private final T target;
    private Predicate<T> skipPredicate;

    /**
     * @param func BiConsumer for applying an interpretation function, where S as a source data
     *     object and T as a target data object
     */
    public Handler<T> via(BiConsumer<S, T> func) {
      Optional.ofNullable(target).ifPresent(t -> func.accept(source, t));
      return this;
    }

    /**
     * @param func Consumer for applying an interpretation function, where T as a source data object
     *     and as a target data object
     */
    public Handler<T> via(Consumer<T> func) {
      Optional.ofNullable(target).ifPresent(func);
      return this;
    }

    /** @param func skips the result if the result of predicate is true */
    public Handler<T> skipWhen(Predicate<T> func) {
      if (skipPredicate == null) {
        skipPredicate = func;
      } else {
        skipPredicate = skipPredicate.and(func);
      }
      return this;
    }

    /** @return target data object */
    public Optional<T> getOfNullable() {
      if (skipPredicate != null && target != null && skipPredicate.test(target)) {
        return Optional.empty();
      }
      return Optional.ofNullable(target);
    }

    /** @return target data object */
    public Optional<T> get() {
      if (skipPredicate != null && skipPredicate.test(target)) {
        return Optional.empty();
      }
      return Optional.of(target);
    }

    /** @param consumer Consumer for consuming target data object */
    public void consume(Consumer<T> consumer) {
      Optional.ofNullable(target).ifPresent(consumer);
    }
  }
}
