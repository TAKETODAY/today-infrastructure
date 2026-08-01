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

import java.io.IOException;

import infra.http.HttpHeaders;
import infra.http.HttpStatusCode;
import infra.lang.Assert;
import infra.web.DecoratingHttpContext;
import infra.web.HttpContext;

/**
 * A response wrapper used for the implementation of
 * {@link RelativeRedirectFilter} also shared with {@link ForwardedHeaderFilter}.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
final class RelativeRedirectResponseWrapper extends DecoratingHttpContext {

  private final HttpStatusCode redirectStatus;

  private RelativeRedirectResponseWrapper(HttpContext delegate, HttpStatusCode redirectStatus) {
    super(delegate);
    Assert.notNull(redirectStatus, "'redirectStatus' is required");
    this.redirectStatus = redirectStatus;
  }

  @Override
  public void sendRedirect(String location) throws IOException {
    reset();
    setStatus(this.redirectStatus.value());
    setHeader(HttpHeaders.LOCATION, location);
    flush();
  }

  public static HttpContext wrapIfNecessary(HttpContext context, HttpStatusCode redirectStatus) {
    RelativeRedirectResponseWrapper wrapper = context.getNativeContext(RelativeRedirectResponseWrapper.class);

    return wrapper != null ? context :
            new RelativeRedirectResponseWrapper(context, redirectStatus);
  }

}
