/**
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

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.context.condition.ConditionalOnClass;
import cn.taketoday.core.Constant;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.handler.AbstractHandlerAdapter;

/**
 * @author TODAY 2019-12-24 22:01
 */
@ConditionalOnClass(Constant.ENV_SERVLET)
public class ServletHandlerAdapter extends AbstractHandlerAdapter {

  public ServletHandlerAdapter() { }

  public ServletHandlerAdapter(int order) {
    setOrder(order);
  }

  @Override
  public boolean supports(Object handler) {
    return handler instanceof Servlet;
  }

  @Override
  public Object handle(RequestContext context, Object handler) throws Throwable {
    HttpServletRequest servletRequest = ServletUtils.getServletRequest(context);
    HttpServletResponse servletResponse = ServletUtils.getServletResponse(context);
    ((Servlet) handler).service(servletRequest, servletResponse);
    return NONE_RETURN_VALUE;
  }

}
