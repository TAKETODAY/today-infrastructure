/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.session;

import org.jspecify.annotations.Nullable;

import infra.lang.Assert;
import infra.web.RequestContext;

/**
 * For request parameter name {@link SessionIdResolver}
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RequestParameterSessionIdResolver implements SessionIdResolver {
  private final String parameterName;

  public RequestParameterSessionIdResolver(String parameterName) {
    Assert.hasText(parameterName, "parameterName is required");
    this.parameterName = parameterName;
  }

  @Nullable
  @Override
  public String getSessionId(RequestContext exchange) {
    // find in request attribute
    Object attribute = exchange.getAttribute(WRITTEN_SESSION_ID_ATTR);
    if (attribute instanceof String sessionId) {
      return sessionId;
    }
    return exchange.getParameter(parameterName);
  }

  @Override
  public void setSessionId(RequestContext exchange, String sessionId) {
    Assert.notNull(sessionId, "sessionId is required");
    exchange.setAttribute(WRITTEN_SESSION_ID_ATTR, sessionId);
  }

  @Override
  public void expireSession(RequestContext exchange) {
    exchange.removeAttribute(WRITTEN_SESSION_ID_ATTR);
  }

}
