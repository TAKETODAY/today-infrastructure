/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.filter;

import infra.http.HttpStatus;
import infra.http.HttpStatusCode;
import infra.util.Assert;
import infra.web.Filter;
import infra.web.FilterChain;
import infra.web.HttpContext;

/**
 * Overrides {@link infra.web.HttpContext#sendRedirect(String)} and handles it by
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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class RelativeRedirectFilter implements Filter {

  private HttpStatusCode redirectStatus = HttpStatus.SEE_OTHER;

  /**
   * Set the default HTTP Status to use for redirects.
   * <p>By default this is {@link HttpStatus#SEE_OTHER}.
   *
   * @param status the 3xx redirect status to use
   */
  public void setRedirectStatus(HttpStatusCode status) {
    Assert.notNull(status, "Property 'redirectStatus' is required");
    if (!status.is3xxRedirection()) {
      throw new IllegalArgumentException("Not a redirect status code: " + status);
    }
    this.redirectStatus = status;
  }

  /**
   * Return the configured redirect status.
   */
  public HttpStatusCode getRedirectStatus() {
    return this.redirectStatus;
  }

  @Override
  public void doFilter(HttpContext context, FilterChain filterChain) throws Exception {
    context = RelativeRedirectResponseWrapper.wrapIfNecessary(context, this.redirectStatus);
    filterChain.doFilter(context);
  }

}
