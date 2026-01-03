/*
 * Copyright 2017 - 2026 the original author or authors.
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

package infra.http.client.config;

import org.apache.hc.client5.http.impl.DefaultRedirectStrategy;
import org.apache.hc.client5.http.protocol.RedirectStrategy;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.jspecify.annotations.Nullable;

import java.net.URI;

/**
 * Adapts {@link HttpRedirects} to an
 * <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HttpComponents</a>
 * {@link RedirectStrategy}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
final class HttpComponentsRedirectStrategy {

  private HttpComponentsRedirectStrategy() {
  }

  static RedirectStrategy get(@Nullable HttpRedirects redirects) {
    if (redirects == null) {
      return DefaultRedirectStrategy.INSTANCE;
    }
    return switch (redirects) {
      case FOLLOW_WHEN_POSSIBLE, FOLLOW -> DefaultRedirectStrategy.INSTANCE;
      case DONT_FOLLOW -> NoFollowRedirectStrategy.INSTANCE;
    };
  }

  /**
   * {@link RedirectStrategy} that never follows redirects.
   */
  private static final class NoFollowRedirectStrategy implements RedirectStrategy {

    private static final RedirectStrategy INSTANCE = new NoFollowRedirectStrategy();

    private NoFollowRedirectStrategy() {
    }

    @Override
    public boolean isRedirected(HttpRequest request, HttpResponse response, HttpContext context) {
      return false;
    }

    @Override
    public @Nullable URI getLocationURI(HttpRequest request, HttpResponse response, HttpContext context) {
      return null;
    }

  }

}
