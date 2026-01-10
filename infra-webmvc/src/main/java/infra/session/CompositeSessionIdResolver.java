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

import java.util.List;

import infra.web.RequestContext;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/5 23:28
 */
final class CompositeSessionIdResolver implements SessionIdResolver {

  final List<SessionIdResolver> resolvers;

  public CompositeSessionIdResolver(List<SessionIdResolver> resolvers) {
    this.resolvers = resolvers;
  }

  @Nullable
  @Override
  public String getSessionId(RequestContext exchange) {
    for (SessionIdResolver resolver : resolvers) {
      String token = resolver.getSessionId(exchange);
      if (token != null) {
        return token;
      }
    }
    return null;
  }

  @Override
  public void setSessionId(RequestContext exchange, String session) {
    for (SessionIdResolver resolver : resolvers) {
      resolver.setSessionId(exchange, session);
    }
  }

  @Override
  public void expireSession(RequestContext context) {
    for (SessionIdResolver resolver : resolvers) {
      String token = resolver.getSessionId(context);
      if (token != null) {
        resolver.expireSession(context);
      }
    }
  }
}

