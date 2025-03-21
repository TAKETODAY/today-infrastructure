/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.core.io;

import java.io.IOException;
import java.util.Objects;

/**
 * Resource Consumer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SmartResourceConsumer
 * @since 4.0 2021/12/18 18:17
 */
@FunctionalInterface
public interface ResourceConsumer {

  /**
   * Performs this operation on the given argument.
   *
   * @param resource the input argument
   */
  void accept(Resource resource) throws IOException;

  /**
   * Returns a composed {@code Consumer} that performs, in sequence, this
   * operation followed by the {@code after} operation. If performing either
   * operation throws an exception, it is relayed to the caller of the
   * composed operation.  If performing this operation throws an exception,
   * the {@code after} operation will not be performed.
   *
   * @param after the operation to perform after this operation
   * @return a composed {@code Consumer} that performs in sequence this
   * operation followed by the {@code after} operation
   * @throws NullPointerException if {@code after} is null
   */
  default ResourceConsumer andThen(ResourceConsumer after) {
    Objects.requireNonNull(after);
    return resource -> {
      accept(resource);
      after.accept(resource);
    };
  }
}
