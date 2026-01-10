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

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.lang.Assert;
import infra.web.RequestContext;
import infra.web.annotation.SessionAttribute;
import infra.web.bind.resolver.AbstractNamedValueResolvingStrategy;
import infra.web.handler.method.ResolvableMethodParameter;

/**
 * resolve attribute from {@link Session}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionAttribute
 * @see Session#getAttribute(String)
 * @since 2019-09-27 22:42
 */
public class SessionAttributeParameterResolver extends AbstractNamedValueResolvingStrategy {

  private final SessionManager sessionManager;

  public SessionAttributeParameterResolver(SessionManager sessionManager, @Nullable ConfigurableBeanFactory beanFactory) {
    super(beanFactory);
    Assert.notNull(sessionManager, "sessionManager is required");
    this.sessionManager = sessionManager;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.hasParameterAnnotation(SessionAttribute.class);
  }

  @Nullable
  @Override
  protected Object resolveName(String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    Session session = sessionManager.getSession(context, false);
    if (session == null) {
      return null;
    }
    return session.getAttribute(name);
  }

}
