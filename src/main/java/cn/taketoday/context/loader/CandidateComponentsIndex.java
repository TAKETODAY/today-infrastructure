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

package cn.taketoday.context.loader;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.core.AntPathMatcher;
import cn.taketoday.core.MultiValueMap;
import cn.taketoday.util.ClassUtils;

/**
 * Provide access to the candidates that are defined in {@code META-INF/today.components}.
 *
 * <p>An arbitrary number of stereotypes can be registered (and queried) on the index: a
 * typical example is the fully qualified name of an annotation that flags the class for
 * a certain use case. The following call returns all the {@code @Component}
 * <b>candidate</b> types for the {@code com.example} package (and its sub-packages):
 * <pre class="code">
 * Set&lt;String&gt; candidates = index.getCandidateTypes(
 *         "com.example", "cn.taketoday.lang.Component");
 * </pre>
 *
 * <p>The {@code type} is usually the fully qualified name of a class, though this is
 * not a rule. Similarly, the {@code stereotype} is usually the fully qualified name of
 * a target type but it can be any marker really.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/9 21:34
 */
public class CandidateComponentsIndex {

  private static final AntPathMatcher pathMatcher = new AntPathMatcher(".");

  private final MultiValueMap<String, Entry> index;

  CandidateComponentsIndex(List<Properties> content) {
    this.index = parseIndex(content);
  }

  private static MultiValueMap<String, Entry> parseIndex(List<Properties> content) {
    MultiValueMap<String, Entry> index = MultiValueMap.fromLinkedHashMap();
    for (Properties entry : content) {
      entry.forEach((type, values) -> {
        String[] stereotypes = ((String) values).split(",");
        for (String stereotype : stereotypes) {
          index.add(stereotype, new Entry((String) type));
        }
      });
    }
    return index;
  }

  /**
   * Return the candidate types that are associated with the specified stereotype.
   *
   * @param basePackage the package to check for candidates
   * @param stereotype the stereotype to use
   * @return the candidate types associated with the specified {@code stereotype}
   * or an empty set if none has been found for the specified {@code basePackage}
   */
  public Set<String> getCandidateTypes(String basePackage, String stereotype) {
    List<Entry> candidates = this.index.get(stereotype);
    if (candidates != null) {
      return candidates.parallelStream()
              .filter(t -> t.match(basePackage))
              .map(t -> t.type)
              .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  private static class Entry {

    public final String type;
    public final String packageName;

    Entry(String type) {
      this.type = type;
      this.packageName = ClassUtils.getPackageName(type);
    }

    public boolean match(String basePackage) {
      if (pathMatcher.isPattern(basePackage)) {
        return pathMatcher.match(basePackage, this.packageName);
      }
      else {
        return this.type.startsWith(basePackage);
      }
    }
  }

}
