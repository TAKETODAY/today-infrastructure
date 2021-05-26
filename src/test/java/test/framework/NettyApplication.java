package test.framework;

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.event.ApplicationEventPublisher;
import cn.taketoday.context.event.EnableMethodEventDriven;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.framework.WebApplication;
import cn.taketoday.framework.reactive.EnableNettyHandling;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.http.HttpHeaders;
import cn.taketoday.web.socket.BinaryMessage;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.EnableWebSocket;
import cn.taketoday.web.socket.TextMessage;
import cn.taketoday.web.socket.WebSocketConfiguration;
import cn.taketoday.web.socket.WebSocketHandler;
import cn.taketoday.web.socket.WebSocketHandlerRegistry;
import cn.taketoday.web.socket.WebSocketSession;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY 2021/3/30 22:41
 */
@Slf4j
@RestController
@RestControllerAdvice
//@EnableRequestPathMapping
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
    System.out.println(context.remoteAddress());

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
  @EnableWebSocket
  @EnableNettyHandling
  @EnableMethodEventDriven
  static class AppConfig implements WebSocketConfiguration {

    @Override
    public void configureWebSocketHandlers(WebSocketHandlerRegistry registry) {
      registry.registerHandler(new WebSocket0(), "/endpoint");
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


