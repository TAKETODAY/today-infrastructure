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
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY 2021/3/30 22:41
 */
@Slf4j
@RestController
@RestControllerAdvice
@Import(NettyApplication.AppConfig.class)
public class NettyApplication {

  public static void main(String[] args) {
    WebApplication.runReactive(NettyApplication.class, args);
  }

  @GET("/index")
  public String index() {
    return "Hello";
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
  public void context(RequestContext context) {
    final String requestURL = context.requestURL();
    System.out.println(requestURL);
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
  @EnableNettyHandling
  @EnableMethodEventDriven
  static class AppConfig {

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

}


