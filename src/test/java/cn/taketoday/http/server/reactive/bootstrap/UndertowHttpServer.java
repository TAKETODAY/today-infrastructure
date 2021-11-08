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

package cn.taketoday.http.server.reactive.bootstrap;

import java.net.InetSocketAddress;

import cn.taketoday.http.server.reactive.UndertowHttpHandlerAdapter;
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
