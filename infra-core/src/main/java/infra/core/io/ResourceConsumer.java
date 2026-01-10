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
