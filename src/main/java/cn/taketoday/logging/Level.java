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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.logging;

/**
 * @author TODAY <br>
 * 2019-11-03 20:29
 */
public enum Level {
  /** for tracing messages that are very verbose */
  TRACE(1),
  /** messages suitable for debugging purposes */
  DEBUG(2),
  /** information messages */
  INFO(3),
  /** warning messages */
  WARN(4),
  /** error messages */
  ERROR(5);

  private final int level;

  Level(int level) {
    this.level = level;
  }

  public boolean isEnabled(Level otherLevel) {
    return level <= otherLevel.level;
  }
}
