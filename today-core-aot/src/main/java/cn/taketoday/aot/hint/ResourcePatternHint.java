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

import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * A hint that describes resources that should be made available at runtime.
 *
 * <p>Each pattern may be a simple path which has a one-to-one mapping to a
 * resource on the classpath, or alternatively may contain the special
 * {@code *} character to indicate a wildcard match. For example:
 * <ul>
 *     <li>{@code file.properties}: matches just the {@code file.properties}
 *         file at the root of the classpath.</li>
 *     <li>{@code com/example/file.properties}: matches just the
 *         {@code file.properties} file in {@code com/example/}.</li>
 *     <li>{@code *.properties}: matches all the files with a {@code .properties}
 *         extension anywhere in the classpath.</li>
 *     <li>{@code com/example/*.properties}: matches all the files with a {@code .properties}
 *         extension in {@code com/example/} and its child directories at any depth.</li>
 *     <li>{@code com/example/*}: matches all the files in {@code com/example/}
 *         and its child directories at any depth.</li>
 * </ul>
 *
 * <p>A resource pattern must not start with a slash ({@code /}) unless it is the
 * root directory.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 4.0
 */
public final class ResourcePatternHint implements ConditionalHint {

  private final String pattern;

  @Nullable
  private final TypeReference reachableType;

  ResourcePatternHint(String pattern, @Nullable TypeReference reachableType) {
    Assert.isTrue(("/".equals(pattern) || !pattern.startsWith("/")),
            () -> "Resource pattern [%s] must not start with a '/' unless it is the root directory"
                    .formatted(pattern));
    this.pattern = pattern;
    this.reachableType = reachableType;
  }

  /**
   * Return the pattern to use for identifying the resources to match.
   *
   * @return the pattern
   */
  public String getPattern() {
    return this.pattern;
  }

  /**
   * Return the regex {@link Pattern} to use for identifying the resources to match.
   *
   * @return the regex pattern
   */
  public Pattern toRegex() {
    String prefix = (this.pattern.startsWith("*") ? ".*" : "");
    String suffix = (this.pattern.endsWith("*") ? ".*" : "");
    String regex = Arrays.stream(this.pattern.split("\\*"))
            .filter(s -> !s.isEmpty())
            .map(Pattern::quote)
            .collect(Collectors.joining(".*", prefix, suffix));
    return Pattern.compile(regex);
  }

  @Nullable
  @Override
  public TypeReference getReachableType() {
    return this.reachableType;
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ResourcePatternHint that = (ResourcePatternHint) o;
    return this.pattern.equals(that.pattern)
            && Objects.equals(this.reachableType, that.reachableType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.pattern, this.reachableType);
  }

}
