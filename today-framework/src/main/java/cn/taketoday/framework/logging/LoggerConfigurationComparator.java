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

import java.util.Comparator;

import cn.taketoday.lang.Assert;

/**
 * An implementation of {@link Comparator} for comparing {@link LoggerConfiguration}s.
 * Sorts the "root" logger as the first logger and then lexically by name after that.
 *
 * @author Ben Hale
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LoggerConfigurationComparator implements Comparator<LoggerConfiguration> {

  private final String rootLoggerName;

  /**
   * Create a new {@link LoggerConfigurationComparator} instance.
   *
   * @param rootLoggerName the name of the "root" logger
   */
  LoggerConfigurationComparator(String rootLoggerName) {
    Assert.notNull(rootLoggerName, "RootLoggerName is required");
    this.rootLoggerName = rootLoggerName;
  }

  @Override
  public int compare(LoggerConfiguration o1, LoggerConfiguration o2) {
    if (this.rootLoggerName.equals(o1.getName())) {
      return -1;
    }
    if (this.rootLoggerName.equals(o2.getName())) {
      return 1;
    }
    return o1.getName().compareTo(o2.getName());
  }

}
