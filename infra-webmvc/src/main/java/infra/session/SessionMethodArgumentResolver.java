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

package infra.session;

import org.jspecify.annotations.Nullable;

import infra.web.RequestContext;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * for {@link Session} Type-based parameter resolving
 * <p>
 * Like following example
 * <pre>
 * {@code
 *  // if request not contains a Session create new one
 *  @GET("/captcha")
 *  public BufferedImage captcha(Session session) {
 *     ...
 *     session.setAttribute(RAND_CODE, randCode);
 *     return image;
 *  }
 *  // Session may be null
 *  @GET("/test")
 *  public void nullable(@Nullable Session session) {
 *     ...
 *     if (session == null) {
 *
 *     }
 *     else {
 *
 *     }
 *  }
 *
 * }
 * </pre>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Session
 * @since 2019-09-27 22:36
 */
public class SessionMethodArgumentResolver extends SessionManagerOperations implements ParameterResolvingStrategy {

  public SessionMethodArgumentResolver(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.isAssignableTo(Session.class);
  }

  @Nullable
  @Override
  public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) {
    // todo type checking?
    if (resolvable.isRequired()) {
      return getSession(context);
    }
    // Nullable
    return getSession(context, false);
  }

}
