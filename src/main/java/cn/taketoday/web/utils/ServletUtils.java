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
package cn.taketoday.web.utils;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import cn.taketoday.web.servlet.ServletRequestContext;

import static cn.taketoday.web.RequestContextHolder.prepareContext;

/**
 * @author TODAY
 * @date 2020/12/8 23:07
 */
public abstract class ServletUtils {
  // context

  public static RequestContext getRequestContext(ServletRequest request, ServletResponse response) {
    return getRequestContext((HttpServletRequest) request, (HttpServletResponse) response);
  }

  public static RequestContext getRequestContext(HttpServletRequest request, HttpServletResponse response) {
    RequestContext context = RequestContextHolder.getContext();
    if (context == null) {
      context = prepareContext(new ServletRequestContext(request, response));
    }
    return context;
  }

}
