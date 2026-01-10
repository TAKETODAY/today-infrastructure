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

package infra.context.annotation;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;

/**
 * MetadataReader Consumer
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MetadataReader
 * @since 4.0 2021/12/18 15:19
 */
@FunctionalInterface
public interface MetadataReaderConsumer {

  /**
   * Performs this operation on the given argument.
   *
   * @param metadataReader the metadata reader for the target class
   * @param factory a factory for obtaining metadata readers
   * for other classes (such as superclasses and interfaces)
   */
  void accept(MetadataReader metadataReader, MetadataReaderFactory factory) throws IOException;

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
   * @see java.util.function.Consumer#andThen(Consumer)
   */
  default MetadataReaderConsumer andThen(MetadataReaderConsumer after) {
    Objects.requireNonNull(after);
    return (metadataReader, factory) -> {
      accept(metadataReader, factory);
      after.accept(metadataReader, factory);
    };
  }

}
