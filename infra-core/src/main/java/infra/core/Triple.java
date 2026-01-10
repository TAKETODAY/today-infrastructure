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
import java.util.Optional;

import infra.util.ObjectUtils;

/**
 * Represents a triad of values
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Triple exhibits value semantics, i.e. two triples are equal if all three components are equal.
 * An example of decomposing it into values:
 *
 * @param <A> type of the first value.
 * @param <B> type of the second value.
 * @param <C> type of the third value.
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/24 14:52
 */
public final class Triple<A extends @Nullable Object, B extends @Nullable Object, C extends @Nullable Object> implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  public final A first;

  public final B second;

  public final C third;

  private Triple(A first, B second, C third) {
    this.first = first;
    this.second = second;
    this.third = third;
  }

  public Triple<A, B, C> withFirst(@Nullable A first) {
    if (first == this.first) {
      return this;
    }
    return new Triple<>(first, second, third);
  }

  public Triple<A, B, C> withSecond(@Nullable B second) {
    if (second == this.second) {
      return this;
    }
    return new Triple<>(first, second, third);
  }

  public Triple<A, B, C> withThird(@Nullable C third) {
    if (third == this.third) {
      return this;
    }
    return new Triple<>(first, second, third);
  }

  public Optional<A> first() {
    return Optional.ofNullable(first);
  }

  public Optional<B> second() {
    return Optional.ofNullable(second);
  }

  public Optional<C> third() {
    return Optional.ofNullable(third);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof Triple<?, ?, ?> triple))
      return false;
    return ObjectUtils.nullSafeEquals(first, triple.first)
            && ObjectUtils.nullSafeEquals(second, triple.second)
            && ObjectUtils.nullSafeEquals(third, triple.third);
  }

  @Override
  public int hashCode() {
    return 31 * (31 * (31 + ObjectUtils.nullSafeHashCode(first))
            + ObjectUtils.nullSafeHashCode(second))
            + ObjectUtils.nullSafeHashCode(third);
  }

  @Override
  public String toString() {
    return "<" + first + "," + second + "," + third + ">";
  }

  public static <A extends @Nullable Object, B extends @Nullable Object, C extends @Nullable Object> Triple<A, B, C> of(@Nullable A first, @Nullable B second, @Nullable C third) {
    return new Triple<>(first, second, third);
  }

}
