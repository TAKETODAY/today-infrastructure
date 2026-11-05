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

package infra.web.bind.resolver;

import infra.web.HttpContext;
import infra.web.handler.method.HandlerParameter;
import infra.web.util.HttpContextUriComponentsBuilder;
import infra.web.util.UriComponentsBuilder;

/**
 * Resolves argument values of type {@link UriComponentsBuilder} or
 * {@link HttpContextUriComponentsBuilder}.
 *
 * <p>The returned instance is initialized via
 * {@link HttpContextUriComponentsBuilder#fromRequest(HttpContext)}.
 * Forwarded headers must be processed before argument resolution.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/18 16:45
 */
public class UriComponentsBuilderParameterStrategy implements ParameterResolvingStrategy {

  @Override
  public boolean supportsParameter(HandlerParameter parameter) {
    return parameter.is(UriComponentsBuilder.class) || parameter.is(HttpContextUriComponentsBuilder.class);
  }

  @Override
  public HttpContextUriComponentsBuilder resolveArgument(HttpContext context, HandlerParameter parameter) {
    return HttpContextUriComponentsBuilder.fromRequest(context);
  }

}
