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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import infra.core.Ordered;
import infra.lang.Assert;
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
