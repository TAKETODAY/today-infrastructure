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
package cn.taketoday.web.servlet;

import cn.taketoday.web.RequestContext;
import cn.taketoday.web.RequestContextHolder;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.ServletResponseWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import static cn.taketoday.web.RequestContextHolder.prepareContext;

/**
 * @author TODAY 2020/12/8 23:07
 * @since 3.0
 */
public abstract class ServletUtils {
  // context

  public static RequestContext getRequestContext(ServletRequest request, ServletResponse response) {
    return getRequestContext((HttpServletRequest) request, (HttpServletResponse) response);
  }

  public static RequestContext getRequestContext(HttpServletRequest request, HttpServletResponse response) {
    RequestContext context = RequestContextHolder.getContext();
    if (context == null) {
      context = new ServletRequestContext(request, response);
      prepareContext(context);
    }
    return context;
  }

  /**
   * Get HttpSession
   *
   * @throws IllegalStateException Not run in servlet
   */
  public static HttpSession getHttpSession(final RequestContext context) {
    return getHttpSession(context, true);
  }

  /**
   * Returns the current <code>HttpSession</code>
   * associated with this request or, if there is no
   * current session and <code>create</code> is true, returns
   * a new session.
   *
   * <p>If <code>create</code> is <code>false</code>
   * and the request has no valid <code>HttpSession</code>,
   * this method returns <code>null</code>.
   *
   * <p>To make sure the session is properly maintained,
   * you must call this method before
   * the response is committed. If the container is using cookies
   * to maintain session integrity and is asked to create a new session
   * when the response is committed, an IllegalStateException is thrown.
   *
   * @param create <code>true</code> to create
   * a new session for this request if necessary;
   * <code>false</code> to return <code>null</code>
   * if there's no current session
   * @return the <code>HttpSession</code> associated
   * with this request or <code>null</code> if
   * <code>create</code> is <code>false</code>
   * and the request has no valid session
   * @throws IllegalStateException Not run in servlet
   * @see #getHttpSession(RequestContext)
   */
  public static HttpSession getHttpSession(final RequestContext context, boolean create) {
    if (context instanceof ServletRequestContext) {
      final HttpServletRequest request = ((ServletRequestContext) context).getRequest();
      return request.getSession(create);
    }
    throw new IllegalStateException("Not run in servlet");
  }

  @SuppressWarnings("unchecked")
  public static <T> T getNativeRequest(ServletRequest request, Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(request)) {
        return (T) request;
      }
      else if (request instanceof ServletRequestWrapper) {
        return getNativeRequest(((ServletRequestWrapper) request).getRequest(), requiredType);
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public static <T> T getNativeResponse(ServletResponse response, Class<T> requiredType) {
    if (requiredType != null) {
      if (requiredType.isInstance(response)) {
        return (T) response;
      }
      else if (response instanceof ServletResponseWrapper) {
        return getNativeResponse(((ServletResponseWrapper) response).getResponse(), requiredType);
      }
    }
    return null;
  }

  public static HttpServletRequest getServletRequest(RequestContext context) {
    if (context instanceof ServletRequestContext) {
      return ((ServletRequestContext) context).getRequest();
    }
    throw new IllegalStateException("Not run in servlet");
  }

  public static HttpServletResponse getServletResponse(RequestContext context) {
    if (context instanceof ServletRequestContext) {
      return ((ServletRequestContext) context).getResponse();
    }
    throw new IllegalStateException("Not run in servlet");
  }

}
