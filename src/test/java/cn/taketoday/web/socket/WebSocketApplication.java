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

import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.annotation.EnableJettyHandling;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.EndpointMapping;
import cn.taketoday.web.socket.annotation.OnClose;
import cn.taketoday.web.socket.annotation.OnError;
import cn.taketoday.web.socket.annotation.OnMessage;
import cn.taketoday.web.socket.annotation.OnOpen;
import cn.taketoday.web.socket.jetty.JettyWebSocketHandlerAdapter;

/**
 * @author TODAY 2021/4/5 13:03
 */
@Configuration
@Import(WebSocketApplication.AppConfig.class)
@EnableWebSocket
//@EnableTomcatHandling
@EnableJettyHandling
//@EnableUndertowHandling
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
    JettyWebSocketHandlerAdapter webSocketHandlerAdapter() {
      return new JettyWebSocketHandlerAdapter();
    }

//
//    @Singleton
//    UndertowWebSocketHandlerAdapter webSocketHandlerAdapter() {
//      return new UndertowWebSocketHandlerAdapter();
//    }

  }

  @Component
  @EndpointMapping("/annotation-endpoint")
  static class AnnotationSocketHandler {

    @OnMessage
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
      System.out.println("handleTextMessage" + message);
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
      System.out.println("onOpen");
    }

    @OnError
    public void onError(WebSocketSession session) {
      System.out.println("onError");
    }

    @OnClose
    public void onClose(WebSocketSession session) {
      System.out.println("OnClose");
    }

    /**
     * <li>optional {@link javax.websocket.Session} parameter</li>
     * <li>optional {@link javax.websocket.EndpointConfig} parameter</li>
     * <li>optional {@link cn.taketoday.web.socket.WebSocketSession} parameter</li>
     * <li>parameters annotated with the {@link javax.websocket.server.PathParam} annotation.</li>
     * <li>parameters annotated with the {@link cn.taketoday.web.annotation.PathVariable} annotation.</li>
     */
    @AfterHandshake
    public void afterHandshake(RequestContext context, WebSocketSession session, int q) {
      System.out.println("afterHandshake");
      System.out.println("q=" + q);
    }

  }

  static class WebSocket0 extends WebSocketHandler {

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
      System.out.println("handleTextMessage" + message);
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
      System.out.println("handleBinaryMessage" + message);
    }

    @Override
    public void afterHandshake(RequestContext context, WebSocketSession session) {
      System.out.println("afterHandshake");
    }

    @Override
    public void onClose(WebSocketSession session, CloseStatus status) {
      System.out.println("onClose " + status);
    }
  }

}

