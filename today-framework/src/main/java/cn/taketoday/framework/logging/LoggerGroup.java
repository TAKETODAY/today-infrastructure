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
import java.util.function.BiConsumer;

/**
 * A single logger group.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public final class LoggerGroup {

  private final String name;

  private final List<String> members;

  private LogLevel configuredLevel;

  LoggerGroup(String name, List<String> members) {
    this.name = name;
    this.members = List.copyOf(members);
  }

  public String getName() {
    return this.name;
  }

  public List<String> getMembers() {
    return this.members;
  }

  public boolean hasMembers() {
    return !this.members.isEmpty();
  }

  public LogLevel getConfiguredLevel() {
    return this.configuredLevel;
  }

  public void configureLogLevel(LogLevel level, BiConsumer<String, LogLevel> configurer) {
    this.configuredLevel = level;
    for (String name : members) {
      configurer.accept(name, level);
    }
  }

}
