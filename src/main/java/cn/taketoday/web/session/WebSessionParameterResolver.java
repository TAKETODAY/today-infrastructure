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

import cn.taketoday.core.Ordered;
import cn.taketoday.core.OrderedSupport;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.resolver.ParameterResolvingStrategy;

/**
 * @author TODAY <br>
 * 2019-09-27 22:36
 */
public class WebSessionParameterResolver
        extends WebSessionManagerSupport implements ParameterResolvingStrategy, Ordered {

  private final OrderedSupport ordered = new OrderedSupport();

  public WebSessionParameterResolver(WebSessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public boolean supportsParameter(ResolvableMethodParameter parameter) {
    return parameter.isAssignableTo(WebSession.class);
  }

  @Nullable
  @Override
  public Object resolveParameter(RequestContext context, ResolvableMethodParameter resolvable) {
    if (resolvable.isRequired()) {
      return getSession(context);
    }
    // Nullable
    return getSession(context, false);
  }

  @Override
  public int getOrder() {
    return ordered.getOrder();
  }

  public void setOrder(int order) {
    ordered.setOrder(order);
  }

}
