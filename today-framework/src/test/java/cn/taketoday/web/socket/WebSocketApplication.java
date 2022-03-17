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
package cn.taketoday.web.socket;

import java.io.IOException;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.lang.Component;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.ResponseBody;
import cn.taketoday.web.framework.WebApplication;
import cn.taketoday.web.socket.annotation.AfterHandshake;
import cn.taketoday.web.socket.annotation.EndpointMapping;
import cn.taketoday.web.socket.annotation.Message;
import cn.taketoday.web.socket.annotation.MessageBody;
import cn.taketoday.web.socket.annotation.OnClose;
import cn.taketoday.web.socket.annotation.OnError;
import cn.taketoday.web.socket.annotation.OnMessage;
import cn.taketoday.web.socket.annotation.OnOpen;
import jakarta.websocket.Session;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author TODAY 2021/4/5 13:03
 */
@Configuration
@Import(WebSocketApplication.AppConfig.class)
@EnableWebSocket
//@EnableTomcatHandling
@RequestMapping
//@EnableJettyHandling
//@EnableUndertowHandling
public class WebSocketApplication {

  public static void main(String[] args) {
    WebApplication.run(WebSocketApplication.class, args);
  }

  @Autowired
  protected RequestContext context;

  @GET("/hello")
  @ResponseBody
  public String hello() {
    final String requestPath = context.getRequestPath();
    System.out.println(requestPath);
    return "Hello";
  }

  @Configuration
  static class AppConfig implements WebSocketConfiguration {

    @Override
    public void configureWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.registerHandler("/endpoint", new WebSocket0());
    }

//    @Singleton
//    JettyWebSocketHandlerAdapter webSocketHandlerAdapter() {
//      return new JettyWebSocketHandlerAdapter();
//    }

//
//    @Singleton
//    UndertowWebSocketHandlerAdapter webSocketHandlerAdapter() {
//      return new UndertowWebSocketHandlerAdapter();
//    }

  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class Body {
    String name;
    int age;
  }

  @Component
  @EndpointMapping("/annotation-endpoint")
  static class AnnotationSocketHandler {

    // @jakarta.websocket.OnMessage
    @OnMessage
    public void handleTextMessage(WebSocketSession session, TextMessage message,
            @Message String text, @Message byte[] bytes,
            @MessageBody Body body, boolean last) throws IOException {
      System.out.println("handleTextMessage" + message);
      System.out.println(text == message.getPayload());
      System.out.println(new String(bytes));
      System.out.println(body);
      System.out.println(last);

      session.sendMessage(message);
    }

    @OnOpen
    public void onOpen(WebSocketSession session, Session stdSession) {
      System.out.println("onOpen");
      if (session instanceof NativeWebSocketSession) {
        System.out.println(stdSession == ((NativeWebSocketSession) session).obtainNativeSession());
      }
    }

    @OnError
    public void onError(WebSocketSession session, Throwable thr) {
      System.out.println("onError");
      thr.printStackTrace();
    }

    @OnClose
    public void onClose(WebSocketSession session, CloseStatus status) {
      System.out.println("OnClose->" + status);
    }

    /**
     * <li>optional {@link jakarta.websocket.Session} parameter</li>
     * <li>optional {@link jakarta.websocket.EndpointConfig} parameter</li>
     * <li>optional {@link cn.taketoday.web.socket.WebSocketSession} parameter</li>
     * <li>parameters annotated with the {@link jakarta.websocket.server.PathParam} annotation.</li>
     * <li>parameters annotated with the {@link cn.taketoday.web.annotation.PathVariable} annotation.</li>
     */
    @AfterHandshake
    public void afterHandshake(RequestContext context, int q, WebSocketSession session) {
      System.out.println("afterHandshake");
      System.out.println("q=" + q);
      System.out.println("session=" + session);
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

