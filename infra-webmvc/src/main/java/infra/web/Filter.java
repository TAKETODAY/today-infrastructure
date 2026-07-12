/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.web;

/**
 * Intercepts incoming HTTP requests at the {@link DispatcherHandler} level, before
 * handler mapping and execution.
 *
 * <p>A {@code Filter} operates on the raw {@link HttpContext}, allowing
 * cross-cutting concerns such as security, rate-limiting, request logging, or
 * header manipulation to be applied globally, regardless of the specific handler
 * or controller that processes the request.
 *
 * <p>Unlike {@link HandlerInterceptor}, which is applied <em>after</em> handler
 * mapping and is tied to a resolved handler, {@code Filter} runs <em>before</em>
 * handler lookup. This makes it suitable for:
 * <ul>
 *   <li>Short-circuiting requests before handler matching (e.g., IP block, maintenance mode)</li>
 *   <li>Modifying the request before it reaches handler mapping (e.g., path rewriting)</li>
 *   <li>Global pre/post processing that must cover all handler types</li>
 * </ul>
 *
 * <p>Filters are detected automatically from the application context and ordered
 * by {@link infra.core.annotation.Order @Order} or {@link infra.core.Ordered Ordered}.
 *
 * <p>This is a {@linkplain FunctionalInterface functional interface} whose
 * functional method is {@link #doFilter(HttpContext, FilterChain)}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see FilterChain
 * @see HandlerInterceptor
 * @see DispatcherHandler
 * @since 5.0
 */
@FunctionalInterface
public interface Filter {

  /**
   * Process the current request, optionally delegating to the next filter
   * in the chain via {@link FilterChain#doFilter(HttpContext)}.
   *
   * @param http the current request context
   * @param chain the filter chain to delegate to
   * @throws Exception if any error occurs during filtering
   */
  void doFilter(HttpContext http, FilterChain chain) throws Exception;

}
