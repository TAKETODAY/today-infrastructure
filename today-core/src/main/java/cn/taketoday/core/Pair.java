/*
 * Copyright 2017 - 2023 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core;

import java.io.Serial;
import java.io.Serializable;
import java.util.Comparator;

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

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
@Experimental
public class Pair<A, B> implements Serializable {

  @SuppressWarnings({ "rawtypes" })
  public static final Pair EMPTY = of(null, null);

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  public final A first;

  @Nullable
  public final B second;

  public Pair(@Nullable A first, @Nullable B second) {
    this.first = first;
    this.second = second;
  }

  @Nullable
  public final A getFirst() {
    return first;
  }

  @Nullable
  public final B getSecond() {
    return second;
  }

  public Pair<A, B> withFirst(@Nullable A first) {
    if (first == this.first) {
      return this;
    }
    return new Pair<>(first, second);
  }

  public Pair<A, B> withSecond(@Nullable B second) {
    if (second == this.second) {
      return this;
    }
    return new Pair<>(first, second);
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
    return ToStringBuilder.from(this)
            .append("first", first)
            .append("second", second)
            .toString();
  }

  // Static

  @SuppressWarnings("unchecked")
  public static <A, B> Pair<A, B> empty() {
    return EMPTY;
  }

  public static <A, B> Pair<A, B> of(@Nullable A first, @Nullable B second) {
    return new Pair<>(first, second);
  }

  @Nullable
  public static <T> T getFirst(@Nullable Pair<T, ?> pair) {
    return pair != null ? pair.first : null;
  }

  @Nullable
  public static <T> T getSecond(@Nullable Pair<?, T> pair) {
    return pair != null ? pair.second : null;
  }

  /**
   * @param <A> first value type (Comparable)
   * @param <B> second value type
   * @return a comparator that compares pair values by first value
   */
  public static <A extends Comparable<? super A>, B> Comparator<Pair<A, B>> comparingFirst() {
    return Comparator.comparing(o -> o.first);
  }

  /**
   * @param <A> first value type
   * @param <B> second value type (Comparable)
   * @return a comparator that compares pair values by second value
   */
  public static <A, B extends Comparable<? super B>> Comparator<Pair<A, B>> comparingSecond() {
    return Comparator.comparing(o -> o.second);
  }

}
