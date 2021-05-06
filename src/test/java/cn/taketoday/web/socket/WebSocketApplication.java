/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.web.socket;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.annotation.EnableUndertowHandling;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.undertow.UndertowWebSocketHandlerAdapter;

/**
 * @author TODAY 2021/4/5 13:03
 */
@Configuration
@Import(WebSocketApplication.AppConfig.class)
@EnableWebSocket
//@EnableTomcatHandling
@EnableUndertowHandling
public class WebSocketApplication {

  public static void main(String[] args) {
    WebApplication.run(WebSocketApplication.class, args);
  }

  @Configuration
  static class AppConfig implements WebSocketConfiguration {

    @Override
    public void configureWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.registerHandler(new WebSocket0(), "/endpoint");
    }

    @Singleton
    UndertowWebSocketHandlerAdapter webSocketHandlerAdapter() {
      return new UndertowWebSocketHandlerAdapter();
    }

  }

  static class WebSocket0 extends AbstractWebSocketHandler implements WebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
      System.out.println(message);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
      System.out.println(message);
    }

    @Override
    public void afterHandshake(RequestContext context) {
      System.out.println("afterHandshake");
    }

    @Override
    public void onClose(WebSocketSession session, CloseStatus status) {

    }
  }

}
