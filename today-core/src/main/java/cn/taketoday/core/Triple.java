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

import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;

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
@Experimental
public class Triple<A, B, C> implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  @Nullable
  public final A first;

  @Nullable
  public final B second;

  @Nullable
  public final C third;

  public Triple(@Nullable A first, @Nullable B second, @Nullable C third) {
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
    return ToStringBuilder.from(this)
            .append("first", first)
            .append("second", second)
            .append("third", third)
            .toString();
  }

  public static <A, B, C> Triple<A, B, C> of(@Nullable A first, @Nullable B second, @Nullable C third) {
    return new Triple<>(first, second, third);
  }

}
