/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.context.annotation;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import cn.taketoday.core.type.classreading.MetadataReader;
import cn.taketoday.core.type.classreading.MetadataReaderFactory;

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
