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

package cn.taketoday.framework.web.embedded.jetty;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;

import java.io.IOException;

import cn.taketoday.framework.web.server.Compression;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Jetty {@code HandlerWrapper} static factory.
 *
 * @author Brian Clozel
 */
final class JettyHandlerWrappers {

  private JettyHandlerWrappers() {
  }

  static HandlerWrapper createGzipHandlerWrapper(Compression compression) {
    GzipHandler handler = new GzipHandler();
    handler.setMinGzipSize((int) compression.getMinResponseSize().toBytes());
    handler.setIncludedMimeTypes(compression.getMimeTypes());
    for (HttpMethod httpMethod : HttpMethod.values()) {
      handler.addIncludedMethods(httpMethod.name());
    }
    return handler;
  }

  static HandlerWrapper createServerHeaderHandlerWrapper(String header) {
    return new ServerHeaderHandler(header);
  }

  /**
   * {@link HandlerWrapper} to add a custom {@code server} header.
   */
  private static class ServerHeaderHandler extends HandlerWrapper {

    private static final String SERVER_HEADER = "server";

    private final String value;

    ServerHeaderHandler(String value) {
      this.value = value;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
      if (!response.getHeaderNames().contains(SERVER_HEADER)) {
        response.setHeader(SERVER_HEADER, this.value);
      }
      super.handle(target, baseRequest, request, response);
    }

  }

}
