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

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.annotation.EnableTomcatHandling;
import cn.taketoday.web.annotation.EnableServletContainerInitializer;
import cn.taketoday.web.annotation.GET;

/**
 * @author TODAY 2021/4/3 11:54
 * @since 3.0
 */
//@RestController
@Import(WebSocketDemo.AppConfig.class)
@EnableTomcatHandling
@EnableServletContainerInitializer
public class WebSocketDemo {

  public static void main(String[] args) {
    WebApplication.run(WebSocketDemo.class, args);
  }

  @GET("/")
  public String hello() {
    return "Hello";
  }

//  @Import(WsSci.class)
//  @Configuration
  static class AppConfig {

  }

  @ServerEndpoint("/endpoint")
  public static class MyEndpoint {

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) {
      System.out.println(session + " onOpen " + config);

    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
      System.out.println(session + " 关闭了 " + closeReason);
    }

    @OnError
    public void onError(Session session, Throwable thr) {
      System.out.println(session + " onError " + thr);
      thr.printStackTrace();
    }

    @OnMessage
    public void onMessage(final String message, final Session session) {
      System.out.println("message: " + message);
      session.getAsyncRemote().sendText(message);
    }
  }

}
