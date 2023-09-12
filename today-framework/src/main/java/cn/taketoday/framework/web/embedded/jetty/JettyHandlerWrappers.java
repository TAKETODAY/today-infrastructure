/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.Callback;

import cn.taketoday.framework.web.server.Compression;

/**
 * Jetty {@code HandlerWrapper} static factory.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class JettyHandlerWrappers {

  private JettyHandlerWrappers() { }

  static Handler.Wrapper createGzipHandlerWrapper(Compression compression) {
    GzipHandler handler = new GzipHandler();
    handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
    handler.setIncludedMimeTypes(compression.getMimeTypes());
    for (HttpMethod httpMethod : HttpMethod.values()) {
      handler.addIncludedMethods(httpMethod.name());
    }
    return handler;
  }

  static Handler.Wrapper createServerHeaderHandlerWrapper(String header) {
    return new ServerHeaderHandler(header);
  }

  /**
   * {@link Handler.Wrapper} to add a custom {@code server} header.
   */
  private static class ServerHeaderHandler extends Handler.Wrapper {

    private static final String SERVER_HEADER = "server";

    private final String value;

    ServerHeaderHandler(String value) {
      this.value = value;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
      HttpFields.Mutable headers = response.getHeaders();
      if (!headers.contains(SERVER_HEADER)) {
        headers.add(SERVER_HEADER, this.value);
      }
      return super.handle(request, response, callback);
    }

  }

}
