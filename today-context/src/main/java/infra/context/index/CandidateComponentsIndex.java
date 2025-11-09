/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.context.index;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import infra.util.AntPathMatcher;
import infra.util.ClassUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;

/**
 * Provide access to the candidates that are defined in {@code META-INF/today.components}
 * component index files (see {@link #CandidateComponentsIndex(List)}) or registered
 * programmatically (see {@link #CandidateComponentsIndex()}).
 *
 * <p>An arbitrary number of stereotypes can be registered (and queried) on the index: a
 * typical example is the fully qualified name of an annotation that flags the class for
 * a certain use case. The following call returns all the {@code @Component}
 * <b>candidate</b> types for the {@code com.example} package (and its sub-packages):
 * <pre class="code">
 * Set&lt;String&gt; candidates = index.getCandidateTypes(
 *         "com.example", "infra.stereotype.Component");
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

  private final Set<String> registeredScans = new LinkedHashSet<>();

  private final MultiValueMap<String, Entry> index = new LinkedMultiValueMap<>();

  private final boolean complete;

  /**
   * Create a new index instance from parsed component index files.
   */
  CandidateComponentsIndex(List<Properties> content) {
    for (Properties entry : content) {
      entry.forEach((type, values) -> {
        String[] stereotypes = ((String) values).split(",");
        for (String stereotype : stereotypes) {
          this.index.add(stereotype, new Entry((String) type));
        }
      });
    }
    this.complete = true;
  }

  /**
   * Create a new index instance for programmatic population.
   *
   * @see #registerScan(String...)
   * @see #registerCandidateType(String, String...)
   * @since 5.0
   */
  public CandidateComponentsIndex() {
    this.complete = false;
  }

  /**
   * Programmatically register the given base packages (or base package patterns)
   * as scanned.
   *
   * @see #registerCandidateType(String, String...)
   * @since 5.0
   */
  public void registerScan(String... basePackages) {
    Collections.addAll(this.registeredScans, basePackages);
  }

  /**
   * Return the registered base packages (or base package patterns).
   *
   * @see #registerScan(String...)
   * @since 5.0
   */
  public Set<String> getRegisteredScans() {
    return this.registeredScans;
  }

  /**
   * Determine whether this index contains an entry for the given base package
   * (or base package pattern).
   *
   * @since 5.0
   */
  public boolean hasScannedPackage(String packageName) {
    return this.complete ||
            this.registeredScans.stream().anyMatch(basePackage -> matchPackage(basePackage, packageName));
  }

  /**
   * Programmatically register one or more stereotypes for the given candidate type.
   * <p>Note that the containing packages for candidates are not automatically
   * considered scanned packages. Make sure to call {@link #registerScan(String...)}
   * with the scan-specific base package accordingly.
   *
   * @see #registerScan(String...)
   * @since 5.0
   */
  public void registerCandidateType(String type, String... stereotypes) {
    for (String stereotype : stereotypes) {
      this.index.add(stereotype, new Entry(type));
    }
  }

  /**
   * Return the registered stereotype packages (or base package patterns).
   *
   * @since 5.0
   */
  public Set<String> getRegisteredStereotypes() {
    return this.index.keySet();
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
      return candidates.stream()
              .filter(entry -> entry.match(basePackage))
              .map(entry -> entry.type)
              .collect(Collectors.toSet());
    }
    return Collections.emptySet();
  }

  private static boolean matchPackage(String basePackage, String packageName) {
    if (pathMatcher.isPattern(basePackage)) {
      return pathMatcher.match(basePackage, packageName);
    }
    else {
      return packageName.equals(basePackage) || packageName.startsWith(basePackage + ".");
    }
  }

  private static class Entry {

    final String type;

    private final String packageName;

    Entry(String type) {
      this.type = type;
      this.packageName = ClassUtils.getPackageName(type);
    }

    public boolean match(String basePackage) {
      return matchPackage(basePackage, this.packageName);
    }
  }

}
