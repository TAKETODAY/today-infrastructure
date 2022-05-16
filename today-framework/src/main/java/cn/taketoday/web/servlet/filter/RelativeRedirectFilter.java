/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.servlet.filter;

import java.io.IOException;

import cn.taketoday.http.HttpStatus;
import cn.taketoday.http.HttpStatusCode;
import cn.taketoday.lang.Assert;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Overrides {@link HttpServletResponse#sendRedirect(String)} and handles it by
 * setting the HTTP status and "Location" headers, which keeps the Servlet
 * container from re-writing relative redirect URLs into absolute ones.
 * Servlet containers are required to do that but against the recommendation of
 * <a href="https://tools.ietf.org/html/rfc7231#section-7.1.2"> RFC 7231 Section 7.1.2</a>,
 * and furthermore not necessarily taking into account "X-Forwarded" headers.
 *
 * <p><strong>Note:</strong> While relative redirects are recommended in the
 * RFC, under some configurations with reverse proxies they may not work.
 *
 * @author Rob Winch
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/27 22:03
 */
public class RelativeRedirectFilter extends OncePerRequestFilter {

  private HttpStatusCode redirectStatus = HttpStatus.SEE_OTHER;

  /**
   * Set the default HTTP Status to use for redirects.
   * <p>By default this is {@link HttpStatus#SEE_OTHER}.
   *
   * @param status the 3xx redirect status to use
   */
  public void setRedirectStatus(HttpStatusCode status) {
    Assert.notNull(status, "Property 'redirectStatus' is required");
    Assert.isTrue(status.is3xxRedirection(), "Not a redirect status code");
    this.redirectStatus = status;
  }

  /**
   * Return the configured redirect status.
   */
  public HttpStatusCode getRedirectStatus() {
    return this.redirectStatus;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
          FilterChain filterChain) throws ServletException, IOException {

    response = RelativeRedirectResponseWrapper.wrapIfNecessary(response, this.redirectStatus);
    filterChain.doFilter(request, response);
  }

}
