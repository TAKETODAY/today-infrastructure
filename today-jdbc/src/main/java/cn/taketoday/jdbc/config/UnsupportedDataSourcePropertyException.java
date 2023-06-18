/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.jdbc.config;

import java.io.Serial;
import java.util.function.Supplier;

/**
 * {@link RuntimeException} thrown from {@link DataSourceBuilder} when an unsupported
 * property is used.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/23 17:35
 */
public class UnsupportedDataSourcePropertyException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 1L;

  UnsupportedDataSourcePropertyException(String message) {
    super(message);
  }

  static void throwIf(boolean test, Supplier<String> message) {
    if (test) {
      throw new UnsupportedDataSourcePropertyException(message.get());
    }
  }

}
