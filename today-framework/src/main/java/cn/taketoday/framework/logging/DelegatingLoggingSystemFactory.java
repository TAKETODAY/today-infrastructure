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

package cn.taketoday.framework.logging;

import java.util.List;
import java.util.function.Function;

/**
 * {@link LoggingSystemFactory} that delegates to other factories.
 *
 * @author Phillip Webb
 * @since 4.0
 */
class DelegatingLoggingSystemFactory implements LoggingSystemFactory {

  private final Function<ClassLoader, List<LoggingSystemFactory>> delegates;

  /**
   * Create a new {@link DelegatingLoggingSystemFactory} instance.
   *
   * @param delegates a function that provides the delegates
   */
  DelegatingLoggingSystemFactory(Function<ClassLoader, List<LoggingSystemFactory>> delegates) {
    this.delegates = delegates;
  }

  @Override
  public LoggingSystem getLoggingSystem(ClassLoader classLoader) {
    var delegates = this.delegates != null ? this.delegates.apply(classLoader) : null;
    if (delegates != null) {
      for (LoggingSystemFactory delegate : delegates) {
        LoggingSystem loggingSystem = delegate.getLoggingSystem(classLoader);
        if (loggingSystem != null) {
          return loggingSystem;
        }
      }
    }
    return null;
  }

}
