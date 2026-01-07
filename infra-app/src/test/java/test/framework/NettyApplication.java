/*
 * Copyright 2017 - 2025 the original author or authors.
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

package test.framework;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import infra.annotation.config.logging.ConditionEvaluationReportLoggingListener;
import infra.app.Application;
import infra.app.ApplicationType;
import infra.app.InfraApplication;
import infra.app.logging.LogLevel;
import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationEventPublisher;
import infra.context.annotation.Configuration;
import infra.context.event.EventListener;
import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.core.style.ToStringBuilder;
import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ResourceUtils;
import infra.util.concurrent.Future;
import infra.web.RequestContext;
import infra.web.annotation.ExceptionHandler;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;
import infra.web.socket.CloseStatus;
import infra.web.socket.WebSocketHandler;
import infra.web.socket.WebSocketMessage;
import infra.web.socket.WebSocketSession;
import infra.web.socket.client.support.NettyWebSocketClient;
import infra.web.socket.config.WebSocketConfigurer;
import infra.web.socket.config.WebSocketHandlerRegistry;
import infra.web.socket.server.HandshakeCapable;

/**
 * @author TODAY 2021/3/30 22:41
 */
@RestController
@InfraApplication
public class NettyApplication {

  static final Logger log = LoggerFactory.getLogger(NettyApplication.class);

  public static void main(String[] args) {
    Application.forBuilder(NettyApplication.class)
            .type(ApplicationType.NETTY_WEB)
            .properties(Map.of("logging.level.root", "debug"))
            .initializers(ConditionEvaluationReportLoggingListener.forLoggingLevel(LogLevel.INFO))
            .run(args);
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
      Resource resource = new ClassPathResource("infra/web/handler/404.png");
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

  static class Body {
    final String name;
    final int age;

    Body(String name, int age) {
      this.name = name;
      this.age = age;
    }

    public int getAge() {
      return age;
    }

    public String getName() {
      return name;
    }
  }

  @Configuration
  static class AppConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.addHandler(new WebSocket0(), "/endpoint")
              .setAllowedOriginPatterns("*");
    }

    @EventListener(MyEvent.class)
    public void event(MyEvent event) {
      log.info("event :{}", event);
    }
  }

  static class MyEvent {
    final String name;

    MyEvent(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return ToStringBuilder.forInstance(this)
              .append("name", name)
              .toString();
    }
  }

  @ExceptionHandler(Throwable.class)
  public void throwable(Throwable throwable) {
    throwable.printStackTrace();
  }

  static class WebSocket0 extends WebSocketHandler implements HandshakeCapable {

    @Override
    protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
      System.out.println("handleTextMessage" + message);
      session.send(message.retain());
      return Future.ok();
    }

    @Override
    protected Future<Void> handleBinaryMessage(WebSocketSession session, WebSocketMessage message) {
      System.out.println("handleBinaryMessage" + message);
      return Future.ok();
    }

    @Override
    public void afterHandshake(RequestContext request, @Nullable WebSocketSession session, @Nullable Throwable failure) {
      System.out.println("afterHandshake");
      request.addCookie("name", "demo");
    }

    @Override
    public Future<Void> onClose(WebSocketSession session, CloseStatus status) {
      System.out.println("onClose " + status);
      return Future.ok();
    }
  }

  static class Client {

    public static void main(String[] args) throws InterruptedException {
      NettyWebSocketClient client = new NettyWebSocketClient();
      client.connect(new WebSocketHandler() {
                @Override
                protected Future<Void> handleTextMessage(WebSocketSession session, WebSocketMessage message) {
                  System.out.println("handleTextMessage: " + message);
                  session.close();
                  return Future.ok();
                }

              }, "ws://localhost:8080/endpoint")
              .onSuccess(session -> {
                session.sendText("hello");
              });

      client.shutdown();
    }

  }

}


