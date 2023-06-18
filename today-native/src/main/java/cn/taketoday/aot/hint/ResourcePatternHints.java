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

package cn.taketoday.aot.hint;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cn.taketoday.lang.Nullable;

/**
 * A collection of {@link ResourcePatternHint} describing whether resources should
 * be made available at runtime using a matching algorithm based on include/exclude
 * patterns.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.0
 */
public final class ResourcePatternHints {

  private final List<ResourcePatternHint> includes;

  private final List<ResourcePatternHint> excludes;

  private ResourcePatternHints(Builder builder) {
    this.includes = new ArrayList<>(builder.includes);
    this.excludes = new ArrayList<>(builder.excludes);
  }

  /**
   * Return the include patterns to use to identify the resources to match.
   *
   * @return the include patterns
   */
  public List<ResourcePatternHint> getIncludes() {
    return this.includes;
  }

  /**
   * Return the exclude patterns to use to identify the resources to match.
   *
   * @return the exclude patterns
   */
  public List<ResourcePatternHint> getExcludes() {
    return this.excludes;
  }

  /**
   * Builder for {@link ResourcePatternHints}.
   */
  public static class Builder {

    private final Set<ResourcePatternHint> includes = new LinkedHashSet<>();

    private final Set<ResourcePatternHint> excludes = new LinkedHashSet<>();

    Builder() {
    }

    /**
     * Include resources matching the specified patterns.
     *
     * @param reachableType the type that should be reachable for this hint to apply
     * @param includes the include patterns (see {@link ResourcePatternHint} documentation)
     * @return {@code this}, to facilitate method chaining
     */
    public Builder includes(@Nullable TypeReference reachableType, String... includes) {
      Arrays.stream(includes)
              .map(this::expandToIncludeDirectories)
              .flatMap(List::stream)
              .map(include -> new ResourcePatternHint(include, reachableType))
              .forEach(this.includes::add);
      return this;
    }

    /**
     * Expand the supplied include pattern into multiple patterns that include
     * all parent directories for the ultimate resource or resources.
     * <p>This is necessary to support classpath scanning within a GraalVM
     * native image.
     *
     * @see <a href="https://github.com/spring-projects/spring-framework/issues/29403">gh-29403</a>
     */
    private List<String> expandToIncludeDirectories(String includePattern) {
      // Resource in root or no explicit subdirectories?
      if (!includePattern.contains("/")) {
        // Include the root directory as well as the pattern
        return List.of("/", includePattern);
      }

      List<String> includePatterns = new ArrayList<>();
      // Ensure the root directory and original pattern are always included
      includePatterns.add("/");
      includePatterns.add(includePattern);
      StringBuilder path = new StringBuilder();
      for (String pathElement : includePattern.split("/")) {
        if (pathElement.isEmpty()) {
          // Skip empty path elements
          continue;
        }
        if (pathElement.contains("*")) {
          // Stop at the first encountered wildcard, since we cannot reliably reason
          // any further about the directory structure below this path element.
          break;
        }
        if (!path.isEmpty()) {
          path.append("/");
        }
        path.append(pathElement);
        includePatterns.add(path.toString());
      }
      return includePatterns;
    }

    /**
     * Include resources matching the specified patterns.
     *
     * @param includes the include patterns (see {@link ResourcePatternHint} documentation)
     * @return {@code this}, to facilitate method chaining
     */
    public Builder includes(String... includes) {
      return includes(null, includes);
    }

    /**
     * Exclude resources matching the specified patterns.
     *
     * @param reachableType the type that should be reachable for this hint to apply
     * @param excludes the exclude patterns (see {@link ResourcePatternHint} documentation)
     * @return {@code this}, to facilitate method chaining
     */
    public Builder excludes(TypeReference reachableType, String... excludes) {
      List<ResourcePatternHint> newExcludes = Arrays.stream(excludes)
              .map(include -> new ResourcePatternHint(include, reachableType)).toList();
      this.excludes.addAll(newExcludes);
      return this;
    }

    /**
     * Exclude resources matching the specified patterns.
     *
     * @param excludes the exclude patterns (see {@link ResourcePatternHint} documentation)
     * @return {@code this}, to facilitate method chaining
     */
    public Builder excludes(String... excludes) {
      return excludes(null, excludes);
    }

    /**
     * Create {@link ResourcePatternHints} based on the state of this
     * builder.
     *
     * @return resource pattern hints
     */
    ResourcePatternHints build() {
      return new ResourcePatternHints(this);
    }

  }

}
