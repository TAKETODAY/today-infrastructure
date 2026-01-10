/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.core;

import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import infra.lang.Contract;
import infra.util.ObjectUtils;

/**
 * Represents a generic pair of two values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Pair exhibits value semantics, i.e. two pairs are equal if both components are equal.
 *
 * An example of decomposing it into values:
 *
 * @param <A> type of the first value.
 * @param <B> type of the second value.
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/24 14:45
 */
public class Pair<A extends @Nullable Object, B extends @Nullable Object> implements Map.Entry<A, B>, Serializable {

  @SuppressWarnings({ "rawtypes" })
  public static final Pair EMPTY = of(null, null);

  @Serial
  private static final long serialVersionUID = 1L;

  public final A first;

  public final B second;

  public Pair(A first, B second) {
    this.first = first;
    this.second = second;
  }

  public final A getFirst() {
    return first;
  }

  public final B getSecond() {
    return second;
  }

  @Override
  public A getKey() {
    return first;
  }

  @Override
  public B getValue() {
    return second;
  }

  @Override
  public B setValue(B value) {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns a new {@code Pair} with the specified first value and the same second value.
   *
   * @param first the first value for the new pair
   * @return a new pair with the given first value and the same second value
   */
  public Pair<A, B> withFirst(A first) {
    if (first == this.first) {
      return this;
    }
    return new Pair<>(first, second);
  }

  /**
   * Returns a new {@code Pair} with the specified second value and the same first value.
   *
   * @param second the second value for the new pair
   * @return a new pair with the given second value and the same first value
   */
  public Pair<A, B> withSecond(B second) {
    if (second == this.second) {
      return this;
    }
    return new Pair<>(first, second);
  }

  /**
   * Returns an {@code Optional} containing the first value, or empty if the first value is null.
   *
   * @return an optional containing the first value
   */
  public Optional<A> first() {
    return Optional.ofNullable(first);
  }

  /**
   * Returns an {@code Optional} containing the second value, or empty if the second value is null.
   *
   * @return an optional containing the second value
   */
  public Optional<B> second() {
    return Optional.ofNullable(second);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Pair<?, ?> pair))
      return false;
    return ObjectUtils.nullSafeEquals(first, pair.first)
            && ObjectUtils.nullSafeEquals(second, pair.second);
  }

  @Override
  public int hashCode() {
    return 31 * (31 + ObjectUtils.nullSafeHashCode(first))
            + ObjectUtils.nullSafeHashCode(second);
  }

  /**
   * Returns string representation of the [Pair] including its [first] and [second] values.
   */
  @Override
  public String toString() {
    return "<" + first + "," + second + ">";
  }

  // Static

  /**
   * Returns an empty {@code Pair} instance with both values being {@code null}.
   *
   * @param <A> the type of the first value
   * @param <B> the type of the second value
   * @return an empty pair with {@code null} values
   */
  @SuppressWarnings("unchecked")
  public static <A extends @Nullable Object, B extends @Nullable Object> Pair<A, B> empty() {
    return EMPTY;
  }

  public static <A extends @Nullable Object, B extends @Nullable Object> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    return new Pair<>(first, second);
  }

  /**
   * Returns the first value from the given pair, or {@code null} if the pair is {@code null}.
   *
   * @param pair the pair to get the first value from
   * @param <T> the type of the first value
   * @return the first value of the pair, or {@code null} if the pair is {@code null}
   */
  @Contract("null -> null")
  public static <T extends @Nullable Object> @Nullable T first(@Nullable Pair<T, ?> pair) {
    return pair != null ? pair.first : null;
  }

  /**
   * Returns the second value from the given pair, or {@code null} if the pair is {@code null}.
   *
   * @param pair the pair to get the second value from
   * @param <T> the type of the second value
   * @return the second value of the pair, or {@code null} if the pair is {@code null}
   */
  @Contract("null -> null")
  public static <T extends @Nullable Object> @Nullable T second(@Nullable Pair<?, T> pair) {
    return pair != null ? pair.second : null;
  }

  /**
   * Returns a comparator that compares {@code Pair} values by their first value.
   *
   * @param <A> the first value type (must be {@code Comparable})
   * @param <B> the second value type
   * @return a comparator that compares pairs by first value
   */
  public static <A extends Comparable<? super A>, B extends @Nullable Object> Comparator<Pair<A, B>> comparingFirst() {
    return Comparator.comparing(Pair::getFirst);
  }

  /**
   * Returns a comparator that compares {@code Pair} values by their second value.
   *
   * @param <A> the first value type
   * @param <B> the second value type (must be {@code Comparable})
   * @return a comparator that compares pairs by second value
   */
  public static <A extends @Nullable Object, B extends Comparable<? super B>> Comparator<Pair<A, B>> comparingSecond() {
    return Comparator.comparing(Pair::getSecond);
  }

}
