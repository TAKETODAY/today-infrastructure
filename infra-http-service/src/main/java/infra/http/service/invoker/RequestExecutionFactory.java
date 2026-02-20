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

import java.lang.reflect.Method;

/**
 * Factory for creating HTTP request executors.
 *
 * @param <T> the type of HTTP request values
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/17 22:42
 */
public interface RequestExecutionFactory<T extends HttpRequestValues> extends HttpRequestValuesCreator<T> {

  /**
   * Creates an HTTP request executor for the given method and return type.
   *
   * @param serviceType service type
   * @param method the method to create executor for
   * @return a new HTTP request executor instance
   */
  RequestExecution<T> createRequestExecution(Class<?> serviceType, Method method);

  /**
   * Whether the underlying client supports use of request attributes.
   */
  boolean supportsRequestAttributes();

}
