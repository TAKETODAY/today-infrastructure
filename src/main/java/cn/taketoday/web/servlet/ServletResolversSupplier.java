/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.servlet;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import cn.taketoday.expression.CompositeExpressionResolver;
import cn.taketoday.expression.ExpressionContext;
import cn.taketoday.expression.ExpressionResolver;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.template.ModelAttributeResolver;
import cn.taketoday.web.view.template.ResolversSupplier;

/**
 * @author TODAY 2021/4/15 22:20
 * @since 3.0
 */
final class ServletResolversSupplier extends ResolversSupplier {

  @Override
  public ExpressionResolver getResolvers(ExpressionContext sharedContext, RequestContext context) {
    if (context instanceof ServletRequestContext) {
      final HttpServletRequest request = ((ServletRequestContext) context).getRequest();
      final HttpSession session = request.getSession(false);
      final ServletContext servletContext = request.getServletContext();

      final ServletRequestModelAdapter servletRequestModelAdapter = new ServletRequestModelAdapter(request);
      final ServletContextModelAdapter servletContextModelAdapter = new ServletContextModelAdapter(servletContext);

      if (session != null) {
        final HttpSessionModelAdapter httpSessionModelAdapter = new HttpSessionModelAdapter(session);
        return new CompositeExpressionResolver(
                new ModelAttributeResolver(context),
                new ModelAttributeResolver(servletRequestModelAdapter), // 1
                new ModelAttributeResolver(httpSessionModelAdapter), // 2
                new ModelAttributeResolver(servletContextModelAdapter), // 3
                sharedContext.getResolver()
        );
      }

      return new CompositeExpressionResolver(
              new ModelAttributeResolver(context),
              new ModelAttributeResolver(servletRequestModelAdapter), // 1
              new ModelAttributeResolver(servletContextModelAdapter), // 2
              sharedContext.getResolver()
      );
    }
    throw new IllegalStateException("Not run in servlet");
  }

}
