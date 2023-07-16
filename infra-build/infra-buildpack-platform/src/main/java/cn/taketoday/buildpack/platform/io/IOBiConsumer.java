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

package cn.taketoday.buildpack.platform.io;

import java.io.IOException;

/**
 * BiConsumer that can safely throw {@link IOException IO exceptions}.
 *
 * @param <T> the first consumed type
 * @param <U> the second consumed type
 * @author Phillip Webb
 * @since 4.0
 */
@FunctionalInterface
public interface IOBiConsumer<T, U> {

  /**
   * Performs this operation on the given argument.
   *
   * @param t the first instance to consume
   * @param u the second instance to consumer
   * @throws IOException on IO error
   */
  void accept(T t, U u) throws IOException;

}
