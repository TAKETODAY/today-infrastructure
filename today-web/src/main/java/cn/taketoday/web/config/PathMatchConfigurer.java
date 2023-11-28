/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web.config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import cn.taketoday.lang.Nullable;

/**
 * Assist with configuring {@code HandlerMapping}'s with path matching options.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 20:41
 */
public class PathMatchConfigurer {

  @Nullable
  private Boolean trailingSlashMatch;

  @Nullable
  private Boolean caseSensitiveMatch;

  @Nullable
  private Map<String, Predicate<Class<?>>> pathPrefixes;

  /**
   * Whether to match to URLs irrespective of their case.
   * If enabled a method mapped to "/users" won't match to "/Users/".
   * <p>The default value is {@code false}.
   */
  public PathMatchConfigurer setUseCaseSensitiveMatch(@Nullable Boolean caseSensitiveMatch) {
    this.caseSensitiveMatch = caseSensitiveMatch;
    return this;
  }

  /**
   * Whether to match to URLs irrespective of the presence of a trailing slash.
   * If enabled a method mapped to "/users" also matches to "/users/".
   * <p>The default value is {@code true}.
   */
  public PathMatchConfigurer setUseTrailingSlashMatch(@Nullable Boolean trailingSlashMatch) {
    this.trailingSlashMatch = trailingSlashMatch;
    return this;
  }

  /**
   * Configure a path prefix to apply to matching controller methods.
   * <p>Prefixes are used to enrich the mappings of every {@code @RequestMapping}
   * method whose controller type is matched by the corresponding
   * {@code Predicate}. The prefix for the first matching predicate is used.
   * <p>Consider using {@link cn.taketoday.web.handler.method.HandlerTypePredicate
   * HandlerTypePredicate} to group controllers.
   *
   * @param prefix the prefix to apply
   * @param predicate a predicate for matching controller types
   */
  public PathMatchConfigurer addPathPrefix(String prefix, Predicate<Class<?>> predicate) {
    if (this.pathPrefixes == null) {
      this.pathPrefixes = new LinkedHashMap<>();
    }
    this.pathPrefixes.put(prefix, predicate);
    return this;
  }

  @Nullable
  protected Map<String, Predicate<Class<?>>> getPathPrefixes() {
    return this.pathPrefixes;
  }

  @Nullable
  public Boolean isUseTrailingSlashMatch() {
    return this.trailingSlashMatch;
  }

  @Nullable
  public Boolean isUseCaseSensitiveMatch() {
    return this.caseSensitiveMatch;
  }

}
