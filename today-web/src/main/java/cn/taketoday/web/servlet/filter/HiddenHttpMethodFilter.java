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

package cn.taketoday.web.servlet.filter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import cn.taketoday.http.HttpMethod;
import cn.taketoday.lang.Assert;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.util.WebUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * {@link jakarta.servlet.Filter} that converts posted method parameters into HTTP methods,
 * retrievable via {@link HttpServletRequest#getMethod()}. Since browsers currently only
 * support GET and POST, a common technique - used by the Prototype library, for instance -
 * is to use a normal POST with an additional hidden form field ({@code _method})
 * to pass the "real" HTTP method along. This filter reads that parameter and changes
 * the {@link HttpServletRequestWrapper#getMethod()} return value accordingly.
 * Only {@code "PUT"}, {@code "DELETE"} and {@code "PATCH"} HTTP methods are allowed.
 *
 * <p>The name of the request parameter defaults to {@code _method}, but can be
 * adapted via the {@link #setMethodParam(String) methodParam} property.
 *
 * <p><b>NOTE: This filter needs to run after multipart processing in case of a multipart
 * POST request, due to its inherent need for checking a POST body parameter.</b>
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:44
 */
@Deprecated
public class HiddenHttpMethodFilter extends OncePerRequestFilter {

  private static final List<String> ALLOWED_METHODS =
          List.of(HttpMethod.PUT.name(), HttpMethod.DELETE.name(), HttpMethod.PATCH.name());

  /** Default method parameter: {@code _method}. */
  public static final String DEFAULT_METHOD_PARAM = "_method";

  private String methodParam = DEFAULT_METHOD_PARAM;

  /**
   * Set the parameter name to look for HTTP methods.
   *
   * @see #DEFAULT_METHOD_PARAM
   */
  public void setMethodParam(String methodParam) {
    Assert.hasText(methodParam, "'methodParam' must not be empty");
    this.methodParam = methodParam;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
          throws ServletException, IOException {

    HttpServletRequest requestToUse = request;

    if ("POST".equals(request.getMethod()) && request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) == null) {
      String paramValue = request.getParameter(this.methodParam);
      if (StringUtils.isNotEmpty(paramValue)) {
        String method = paramValue.toUpperCase(Locale.ENGLISH);
        if (ALLOWED_METHODS.contains(method)) {
          requestToUse = new HttpMethodRequestWrapper(request, method);
        }
      }
    }

    filterChain.doFilter(requestToUse, response);
  }

  /**
   * Simple {@link HttpServletRequest} wrapper that returns the supplied method for
   * {@link HttpServletRequest#getMethod()}.
   */
  private static class HttpMethodRequestWrapper extends HttpServletRequestWrapper {

    private final String method;

    public HttpMethodRequestWrapper(HttpServletRequest request, String method) {
      super(request);
      this.method = method;
    }

    @Override
    public String getMethod() {
      return this.method;
    }
  }

}

