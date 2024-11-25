/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.app.logging;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import infra.lang.Nullable;

/**
 * Logger groups configured through the Infra environment.
 *
 * @author HaiTao Zhang
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 #see {@link LoggerGroup}
 */
public final class LoggerGroups implements Iterable<LoggerGroup> {

  private final ConcurrentHashMap<String, LoggerGroup> groups = new ConcurrentHashMap<>();

  public LoggerGroups() { }

  public LoggerGroups(Map<String, List<String>> namesAndMembers) {
    putAll(namesAndMembers);
  }

  public void putAll(Map<String, List<String>> namesAndMembers) {
    for (Map.Entry<String, List<String>> entry : namesAndMembers.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  private void put(String name, List<String> members) {
    put(new LoggerGroup(name, members));
  }

  private void put(LoggerGroup loggerGroup) {
    this.groups.put(loggerGroup.getName(), loggerGroup);
  }

  @Nullable
  public LoggerGroup get(String name) {
    return this.groups.get(name);
  }

  @Override
  public Iterator<LoggerGroup> iterator() {
    return this.groups.values().iterator();
  }

}
