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

package infra.aot.hint;

import java.util.Objects;

import infra.util.AntPathMatcher;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * A hint that describes resources that should be made available at runtime.
 *
 * <p>Each pattern may be a simple path which has a one-to-one mapping to a
 * resource on the classpath, or alternatively may contain the special
 * {@code *} character to indicate a wildcard match. For example:
 * <ul>
 *     <li>"file.properties": matches just the {@code file.properties}
 *         file at the root of the classpath.</li>
 *     <li>"com/example/file.properties": matches just the
 *         {@code file.properties} file in {@code com/example/}.</li>
 *     <li>"*.properties": matches all the files with a {@code .properties}
 *         extension at the root of the classpath.</li>
 *     <li>"com/example/*.properties": matches all the files with a {@code .properties}
 *         extension in {@code com/example/}.</li>
 *     <li>"com/example/{@literal **}": matches all the files in {@code com/example/}
 *         and its child directories at any depth.</li>
 *     <li>"com/example/{@literal **}/*.properties": matches all the files with a {@code .properties}
 *         extension in {@code com/example/} and its child directories at any depth.</li>
 * </ul>
 *
 * <p>A resource pattern must not start with a slash ({@code /}) unless it is the
 * root directory.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 4.0
 */
public final class ResourcePatternHint implements ConditionalHint {

  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final String pattern;

  private final @Nullable TypeReference reachableType;

  ResourcePatternHint(String pattern, @Nullable TypeReference reachableType) {
    Assert.isTrue(("/".equals(pattern) || !pattern.startsWith("/")),
            () -> "Resource pattern [%s] must not start with a '/' unless it is the root directory"
                    .formatted(pattern));
    this.pattern = pattern;
    this.reachableType = reachableType;
  }

  /**
   * Return the pattern to use for identifying the resources to match.
   */
  public String getPattern() {
    return this.pattern;
  }

  /**
   * Whether the given path matches the current glob pattern.
   *
   * @param path the path to match against
   */
  public boolean matches(String path) {
    return PATH_MATCHER.match(this.pattern, path);
  }

  @Override
  public @Nullable TypeReference getReachableType() {
    return this.reachableType;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    return (this == other || (other instanceof ResourcePatternHint that &&
            this.pattern.equals(that.pattern) && Objects.equals(this.reachableType, that.reachableType)));
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.pattern, this.reachableType);
  }

}
