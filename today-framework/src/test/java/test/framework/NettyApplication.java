/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package test.framework;

import java.io.IOException;

import javax.imageio.ImageIO;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.framework.web.WebApplication;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.http.MediaType;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketSession;
import cn.taketoday.web.socket.config.WebSocketConfigurer;
import cn.taketoday.web.socket.config.WebSocketHandlerRegistry;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY 2021/3/30 22:41
 */
@Slf4j
@EnableWebMvc
@RestController
@RestControllerAdvice
@Import(NettyApplication.AppConfig.class)
public class NettyApplication {

  public static void main(String[] args) {
    WebApplication.runReactive(NettyApplication.class, args);
//    WebApplication.run(NettyApplication.class, args);
  }

  @GET("/index")
  public String index() {
    return "Hello";
  }

  @GET("/")
  public String hello() {
    return "Hello";
  }

  @GET("/object")
  public Object object(boolean key1, boolean key2, boolean key3, RequestContext context) throws IOException {
    if (key1) {
      return new Body("key1", 1);
    }
    if (key2) {
      Resource resource = new ClassPathResource("cn/taketoday/web/handler/404.png");
      context.setContentType(MediaType.IMAGE_JPEG_VALUE);
      return ImageIO.read(resource.getInputStream());
    }
    if (key3) {
      return ResourceUtils.getResource("classpath:application.yaml");
    }
    return "body:Hello";
  }

  @GET("/header")
  public HttpHeaders header(HttpHeaders headers) {
    return headers;
  }

  @GET("/body/{name}/{age}")
  public Body index(String name, int age) {
    return new Body(name, age);
  }

  @GET("/publish-event")
  public void index(String name, @Autowired ApplicationEventPublisher publisher) {
    publisher.publishEvent(new MyEvent(name));
  }

  @GET("/request-context")
  public String context(RequestContext context) {
    final String requestURL = context.getRequestURL();
    final String queryString = context.getQueryString();
    System.out.println(requestURL);
    System.out.println(queryString);
    System.out.println(context.getRemoteAddress());

    return queryString;
  }

  @Getter
  static class Body {
    final String name;
    final int age;

    Body(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }

  @Configuration
  static class AppConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(new WebSocket0(), "/endpoint");
    }

    @EventListener(MyEvent.class)
    public void event(MyEvent event) {
      log.info("event :{}", event);
    }
  }

  @ToString
  static class MyEvent {
    final String name;

    MyEvent(String name) {
      this.name = name;
    }
  }

  @ExceptionHandler(Throwable.class)
  public void throwable(Throwable throwable) {
    throwable.printStackTrace();
  }

  static class WebSocket0 extends WebSocketHandler {

    @SneakyThrows
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
      System.out.println("handleTextMessage" + message);
      session.sendMessage(message);
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


