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

package cn.taketoday.web.testfixture.http.server.reactive.bootstrap;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.http.server.reactive.ReactorNetty2HttpHandlerAdapter;
import reactor.netty5.DisposableServer;

/**
 * This class is copied from {@link ReactorHttpServer}.
 *
 * @author Violeta Georgieva
 * @since 4.0
 */
public class ReactorNetty2HttpServer extends AbstractHttpServer {

  private ReactorNetty2HttpHandlerAdapter reactorHandler;

  private reactor.netty5.http.server.HttpServer reactorServer;

  private AtomicReference<DisposableServer> serverRef = new AtomicReference<>();

  @Override
  protected void initServer() {
    this.reactorHandler = createHttpHandlerAdapter();
    this.reactorServer = reactor.netty5.http.server.HttpServer.create().wiretap(true)
            .host(getHost()).port(getPort());
  }

  private ReactorNetty2HttpHandlerAdapter createHttpHandlerAdapter() {
    return new ReactorNetty2HttpHandlerAdapter(resolveHttpHandler());
  }

  @Override
  protected void startInternal() {
    DisposableServer server = this.reactorServer.handle(this.reactorHandler).bind().block();
    setPort(((InetSocketAddress) server.address()).getPort());
    this.serverRef.set(server);
  }

  @Override
  protected void stopInternal() {
    this.serverRef.get().dispose();
  }

  @Override
  protected void resetInternal() {
    this.reactorServer = null;
    this.reactorHandler = null;
    this.serverRef.set(null);
  }

}
