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

package infra.http.service.invoker;

import org.jspecify.annotations.Nullable;

/**
 * Interface for executing HTTP requests with given request values.
 * Implementations of this interface are responsible for handling the execution
 * of HTTP requests and returning the result.
 *
 * @param <T> the type of request values, must extend {@link HttpRequestValues}
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see RequestExecutionFactory
 * @since 5.0 2026/1/17 22:40
 */
public interface RequestExecution<T extends HttpRequestValues> {

  /**
   * Executes the HTTP request with the provided request values.
   *
   * @param requestValues the request values containing all necessary information for the request
   * @return the result of the request execution, or {@code null} if no result is returned
   */
  @Nullable
  Object execute(T requestValues);

}
