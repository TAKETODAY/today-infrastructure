/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.config.annotation;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import infra.web.handler.method.HandlerTypePredicate;

/**
 * Assist with configuring {@code HandlerMapping}'s with path matching options.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/19 20:41
 */
public class PathMatchConfigurer {

  private @Nullable Boolean trailingSlashMatch;

  private @Nullable Boolean caseSensitiveMatch;

  private @Nullable Map<String, Predicate<Class<?>>> pathPrefixes;

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
   * <p>Consider using {@link HandlerTypePredicate
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
