/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.core.conversion;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A converter converts a source object of type {@code S} to a target of type {@code T}.
 *
 * <p>Implementations of this interface are thread-safe and can be shared.
 *
 * @param <S> the source type
 * @param <T> the target type
 * @author TODAY 2018-07-07 21:33:52
 */
@FunctionalInterface
public interface Converter<S, T> {

  /**
   * Convert the source object of type {@code S} to target type {@code T}.
   *
   * @param source the source object to convert, which must be an instance of {@code S} (never {@code null})
   * @return the converted object, which must be an instance of {@code T} (potentially {@code null})
   * @throws IllegalArgumentException if the source cannot be converted to the desired target type
   */
  @Nullable
  T convert(S source);

  /**
   * Construct a composed {@link Converter} that first applies this {@link Converter}
   * to its input, and then applies the {@code after} {@link Converter} to the
   * result.
   *
   * @param after the {@link Converter} to apply after this {@link Converter}
   * is applied
   * @param <U> the type of output of both the {@code after} {@link Converter}
   * and the composed {@link Converter}
   * @return a composed {@link Converter} that first applies this {@link Converter}
   * and then applies the {@code after} {@link Converter}
   * @since 4.0
   */
  default <U> Converter<S, U> andThen(Converter<? super T, ? extends U> after) {
    Assert.notNull(after, "After Converter is required");
    return (S s) -> {
      T initialResult = convert(s);
      return initialResult != null ? after.convert(initialResult) : null;
    };
  }

}
