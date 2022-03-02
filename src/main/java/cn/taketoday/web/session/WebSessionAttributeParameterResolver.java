/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.web.session;

import cn.taketoday.beans.factory.support.ConfigurableBeanFactory;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.SessionAttribute;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.bind.resolver.AbstractNamedValueResolvingStrategy;

/**
 * @author TODAY <br>
 * @see SessionAttribute
 * @since 2019-09-27 22:42
 */
public class WebSessionAttributeParameterResolver extends AbstractNamedValueResolvingStrategy {

  private final WebSessionManager sessionManager;

  public WebSessionAttributeParameterResolver(WebSessionManager sessionManager) {
    this(sessionManager, null);
  }

  public WebSessionAttributeParameterResolver(WebSessionManager sessionManager, ConfigurableBeanFactory beanFactory) {
    super(beanFactory);
    Assert.notNull(sessionManager, "sessionManager must not be null");
    this.sessionManager = sessionManager;
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.hasParameterAnnotation(SessionAttribute.class);
  }

  @Nullable
  @Override
  protected Object resolveName(
          String name, ResolvableMethodParameter resolvable, RequestContext context) throws Exception {
    WebSession session = sessionManager.getSession(context, false);
    if (session == null) {
      return null;
    }
    return session.getAttribute(name);
  }

}
