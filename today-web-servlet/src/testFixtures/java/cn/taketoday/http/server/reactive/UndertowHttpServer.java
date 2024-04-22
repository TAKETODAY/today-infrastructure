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

package cn.taketoday.http.server.reactive;

import java.net.InetSocketAddress;

import cn.taketoday.web.testfixture.http.server.reactive.bootstrap.AbstractHttpServer;
import io.undertow.Undertow;

/**
 * @author Marek Hawrylczak
 */
public class UndertowHttpServer extends AbstractHttpServer {

  private Undertow server;

  @Override
  protected void initServer() throws Exception {
    this.server = Undertow.builder().addHttpListener(getPort(), getHost())
            .setHandler(initHttpHandlerAdapter())
            .build();
  }

  private UndertowHttpHandlerAdapter initHttpHandlerAdapter() {
    return new UndertowHttpHandlerAdapter(resolveHttpHandler());
  }

  @Override
  protected void startInternal() {
    this.server.start();
    Undertow.ListenerInfo info = this.server.getListenerInfo().get(0);
    setPort(((InetSocketAddress) info.getAddress()).getPort());
  }

  @Override
  protected void stopInternal() {
    this.server.stop();
  }

  @Override
  protected void resetInternal() {
    this.server = null;
  }

}
