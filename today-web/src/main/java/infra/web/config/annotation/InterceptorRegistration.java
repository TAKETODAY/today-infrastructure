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

package infra.web.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.core.Ordered;
import infra.lang.Assert;
import infra.lang.Nullable;
import infra.util.StringUtils;
import infra.web.HandlerInterceptor;
import infra.web.handler.MappedInterceptor;
import infra.web.util.pattern.PathPattern;

/**
 * Assists with the creation of a {@link MappedInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/8/30 21:38
 */
public class InterceptorRegistration implements Ordered {

  private final HandlerInterceptor interceptor;

  @Nullable
  private List<String> includePatterns;

  @Nullable
  private List<String> excludePatterns;

  private int order = 0;

  /**
   * Create an {@link InterceptorRegistration} instance.
   */
  public InterceptorRegistration(HandlerInterceptor interceptor) {
    Assert.notNull(interceptor, "Interceptor is required");
    this.interceptor = interceptor;
  }

  /**
   * Add patterns for URLs the interceptor should be included in.
   * <p>For pattern syntax see {@link PathPattern} The syntax is largely the same with
   * {@link PathPattern} more tailored for web usage and more efficient.
   */
  public InterceptorRegistration addPathPatterns(String... patterns) {
    return addPathPatterns(Arrays.asList(patterns));
  }

  /**
   * List-based variant of {@link #addPathPatterns(String...)}.
   */
  public InterceptorRegistration addPathPatterns(List<String> patterns) {
    this.includePatterns = includePatterns != null ?
                           includePatterns : new ArrayList<>(patterns.size());
    this.includePatterns.addAll(patterns);
    return this;
  }

  /**
   * Add patterns for URLs the interceptor should be excluded from.
   * <p>For pattern syntax see {@link PathPattern} The syntax is largely the same with
   * {@link PathPattern} more tailored for web usage and more efficient.
   */
  public InterceptorRegistration excludePathPatterns(String... patterns) {
    return excludePathPatterns(Arrays.asList(patterns));
  }

  /**
   * List-based variant of {@link #excludePathPatterns(String...)}.
   */
  public InterceptorRegistration excludePathPatterns(List<String> patterns) {
    this.excludePatterns = excludePatterns != null ?
                           excludePatterns : new ArrayList<>(patterns.size());
    this.excludePatterns.addAll(patterns);
    return this;
  }

  /**
   * Specify an order position to be used. Default is 0.
   */
  public InterceptorRegistration order(int order) {
    this.order = order;
    return this;
  }

  /**
   * Return the order position to be used.
   */
  @Override
  public int getOrder() {
    return this.order;
  }

  /**
   * Build the underlying interceptor. If URL patterns are provided, the returned
   * type is {@link MappedInterceptor}; otherwise {@link HandlerInterceptor}.
   */
  protected Object getInterceptor() {
    if (this.includePatterns == null && this.excludePatterns == null) {
      return this.interceptor;
    }

    return new MappedInterceptor(
            StringUtils.toStringArray(includePatterns),
            StringUtils.toStringArray(excludePatterns), interceptor);
  }

}
