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

package cn.taketoday.web.socket.undertow;

import org.xnio.FinishedIoFuture;
import org.xnio.FutureResult;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.taketoday.http.HttpHeaders;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletUtils;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.HttpUpgradeListener;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.spi.WebSocketHttpExchange;

/**
 * @author TODAY 2021/5/6 19:32
 * @since 3.0.1
 */
public class UndertowWebSocketHttpExchange implements WebSocketHttpExchange {

  private final RequestContext context;
  private final HttpServerExchange exchange;
  private final Set<WebSocketChannel> peerConnections;

  static ServletRequestContext requireCurrentServletRequestContext() {
    return ServletRequestContext.requireCurrent();
  }

  public UndertowWebSocketHttpExchange(
          RequestContext context, Set<WebSocketChannel> peerConnections) {
    this.context = context;
    this.peerConnections = peerConnections;
    this.exchange = requireCurrentServletRequestContext().getOriginalRequest().getExchange();
  }

  @Override
  public <T> void putAttachment(final AttachmentKey<T> key, final T value) {
    exchange.putAttachment(key, value);
  }

  @Override
  public <T> T getAttachment(final AttachmentKey<T> key) {
    return exchange.getAttachment(key);
  }

  @Override
  public String getRequestHeader(final String headerName) {
    return context.requestHeaders().getFirst(headerName);
  }

  @Override
  public Map<String, List<String>> getRequestHeaders() {
    return context.requestHeaders();
  }

  @Override
  public String getResponseHeader(final String headerName) {
    return context.responseHeaders().getFirst(headerName);
  }

  @Override
  public Map<String, List<String>> getResponseHeaders() {
    return context.responseHeaders();
  }

  @Override
  public void setResponseHeaders(final Map<String, List<String>> headers) {
    final HttpHeaders responseHeaders = context.responseHeaders();
    headers.forEach(responseHeaders::addAll);
  }

  @Override
  public void setResponseHeader(final String headerName, final String headerValue) {
    context.responseHeaders().set(headerName, headerValue);
  }

  @Override
  public void upgradeChannel(final HttpUpgradeListener upgradeCallback) {
    exchange.upgradeChannel(upgradeCallback);
  }

  @Override
  public IoFuture<Void> sendData(final ByteBuffer data) {
    try {
      final OutputStream outputStream = context.getOutputStream();
      while (data.hasRemaining()) {
        outputStream.write(data.get());
      }
      return new FinishedIoFuture<>(null);
    }
    catch (IOException e) {
      final FutureResult<Void> ioFuture = new FutureResult<>();
      ioFuture.setException(e);
      return ioFuture.getIoFuture();
    }
  }

  @Override
  public IoFuture<byte[]> readRequestData() {
    final ByteArrayOutputStream data = new ByteArrayOutputStream();
    try {
      final InputStream in = context.getInputStream();
      byte[] buf = new byte[1024];
      int r;
      while ((r = in.read(buf)) != -1) {
        data.write(buf, 0, r);
      }
      return new FinishedIoFuture<>(data.toByteArray());
    }
    catch (IOException e) {
      final FutureResult<byte[]> ioFuture = new FutureResult<>();
      ioFuture.setException(e);
      return ioFuture.getIoFuture();
    }
  }

  @Override
  public void endExchange() {
    //noop
  }

  @Override
  public void close() {
    IoUtils.safeClose(exchange.getConnection());
  }

  @Override
  public String getRequestScheme() {
    return context.getScheme();
  }

  @Override
  public String getRequestURI() {
    return context.getRequestURL();
  }

  @Override
  public ByteBufferPool getBufferPool() {
    return exchange.getConnection().getByteBufferPool();
  }

  @Override
  public String getQueryString() {
    return context.getQueryString();
  }

  @Override
  public Object getSession() {
    return ServletUtils.getHttpSession(context, false);
  }

  @Override
  public Map<String, List<String>> getRequestParameters() {
    return Collections.emptyMap();
  }

  @Override
  public Principal getUserPrincipal() {
    return ServletUtils.getServletRequest(context).getUserPrincipal();
  }

  @Override
  public boolean isUserInRole(String role) {
    return ServletUtils.getServletRequest(context).isUserInRole(role);
  }

  @Override
  public Set<WebSocketChannel> getPeerConnections() {
    return peerConnections;
  }

  @Override
  public OptionMap getOptions() {
    return exchange.getConnection().getUndertowOptions();
  }
}
