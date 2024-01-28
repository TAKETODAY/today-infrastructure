/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.handler.condition;

import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;

/**
 * Contract for request mapping conditions.
 *
 * <p>Request conditions can be combined via {@link #combine(Object)}, matched to
 * a request via {@link #getMatchingCondition(RequestContext)}, and compared
 * to each other via {@link #compareTo(Object, RequestContext)} to determine
 * which is a closer match for a given request.
 *
 * @param <T> the type of objects that this RequestCondition can be combined
 * with and compared to
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface RequestCondition<T> {

  /**
   * Combine this condition with another such as conditions from a
   * type-level and method-level {@code @RequestMapping} annotation.
   *
   * @param other the condition to combine with.
   * @return a request condition instance that is the result of combining
   * the two condition instances.
   */
  T combine(T other);

  /**
   * Check if the condition matches the request returning a potentially new
   * instance created for the current request. For example a condition with
   * multiple URL patterns may return a new instance only with those patterns
   * that match the request.
   * <p>For CORS pre-flight requests, conditions should match to the would-be,
   * actual request (e.g. URL pattern, query parameters, and the HTTP method
   * from the "Access-Control-Request-Method" header). If a condition cannot
   * be matched to a pre-flight request it should return an instance with
   * empty content thus not causing a failure to match.
   *
   * @param request the current request context
   * @return a condition instance in case of a match or {@code null} otherwise.
   */
  @Nullable
  T getMatchingCondition(RequestContext request);

  /**
   * Compare this condition to another condition in the context of
   * a specific request. This method assumes both instances have
   * been obtained via {@link #getMatchingCondition(RequestContext)}
   * to ensure they have content relevant to current request only.
   *
   * @param request the current request context
   */
  int compareTo(T other, RequestContext request);

}
