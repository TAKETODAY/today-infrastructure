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

package infra.web.service.invoker;

import org.jspecify.annotations.Nullable;

import infra.core.MethodParameter;
import infra.http.HttpMethod;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * {@link HttpServiceArgumentResolver} that resolves the target
 * request's HTTP method from an {@link HttpMethod} argument.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class HttpMethodArgumentResolver implements HttpServiceArgumentResolver {

  private static final Logger logger = LoggerFactory.getLogger(HttpMethodArgumentResolver.class);

  @Override
  public boolean resolve(@Nullable Object argument, MethodParameter parameter, HttpRequestValues.Builder requestValues) {
    if (!parameter.getParameterType().equals(HttpMethod.class)) {
      return false;
    }

    Assert.notNull(argument, "HttpMethod is required");
    HttpMethod httpMethod = (HttpMethod) argument;
    requestValues.setHttpMethod(httpMethod);
    if (logger.isTraceEnabled()) {
      logger.trace("Resolved HTTP method to: {}", httpMethod.name());
    }

    return true;
  }

}
