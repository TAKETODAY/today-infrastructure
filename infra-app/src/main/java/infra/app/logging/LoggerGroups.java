/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.logging;

import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
