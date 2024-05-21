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

package cn.taketoday.session;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.bind.resolver.AbstractNamedValueResolvingStrategy;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;

/**
 * resolve attribute from {@link WebSession}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see SessionAttribute
 * @see WebSession#getAttribute(String)
 * @since 2019-09-27 22:42
 */
public class WebSessionAttributeParameterResolver extends AbstractNamedValueResolvingStrategy {

  private final SessionManager sessionManager;

  public WebSessionAttributeParameterResolver(
          SessionManager sessionManager, @Nullable ConfigurableBeanFactory beanFactory) {
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
    WebSession session = sessionManager.getSession(context, false);
    if (session == null) {
      return null;
    }
    return session.getAttribute(name);
  }

}
