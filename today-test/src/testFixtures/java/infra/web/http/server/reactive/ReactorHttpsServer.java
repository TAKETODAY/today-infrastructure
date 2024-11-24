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

package infra.web.http.server.reactive;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import infra.http.server.reactive.ReactorHttpHandlerAdapter;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import reactor.netty.DisposableServer;
import reactor.netty.http.Http11SslContextSpec;

/**
 * @author Stephane Maldini
 */
public class ReactorHttpsServer extends AbstractHttpServer {

  private ReactorHttpHandlerAdapter reactorHandler;

  private reactor.netty.http.server.HttpServer reactorServer;

  private AtomicReference<DisposableServer> serverRef = new AtomicReference<>();

  @Override
  protected void initServer() throws Exception {
    SelfSignedCertificate cert = new SelfSignedCertificate();
    Http11SslContextSpec http11SslContextSpec = Http11SslContextSpec.forServer(cert.certificate(), cert.privateKey());

    this.reactorHandler = createHttpHandlerAdapter();
    this.reactorServer = reactor.netty.http.server.HttpServer.create()
            .host(getHost())
            .port(getPort())
            .secure(sslContextSpec -> sslContextSpec.sslContext(http11SslContextSpec));
  }

  private ReactorHttpHandlerAdapter createHttpHandlerAdapter() {
    return new ReactorHttpHandlerAdapter(resolveHttpHandler());
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
