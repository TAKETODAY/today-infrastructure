/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

import java.io.PrintWriter;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ApplicationEventPublisher;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.event.EventListener;
import cn.taketoday.framework.InfraApplication;
import cn.taketoday.framework.builder.ApplicationBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.annotation.ExceptionHandler;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.context.async.DeferredResult;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/22 12:52
 */
@Slf4j
@EnableWebMvc
@InfraApplication
@RestController
@RestControllerAdvice
public class RunInServletDemoApplication {

  public static void main(String[] args) {
    ApplicationBuilder.from(RunInServletDemoApplication.class)
            .run(args);
  }

  @Autowired
  private Executor executor;

  @GET
  public String home() {
    return "Home";
  }

  @GET("/index")
  public String index() {
    return "Hello";
  }

  @GET("/body/{name}/{age}")
  public Body body(String name, int age) {
    return new Body(name, age);
  }

  @GET("/publish-event")
  public void index(String name, @Autowired ApplicationEventPublisher publisher) {
    publisher.publishEvent(new MyEvent(name));
  }

  @GET("/request-context")
  public String context(RequestContext context) {
    String requestURL = context.getRequestURL();
    String queryString = context.getQueryString();
    System.out.println(requestURL);
    System.out.println(queryString);

    return queryString;
  }

  @GET("/deferred-result")
  public DeferredResult<String> deferredResult(@Nullable Long timeout) {
    DeferredResult<String> result = new DeferredResult<>(timeout, "Timeout");
    executor.execute(() -> {
      ExceptionUtils.sneakyThrow(() -> TimeUnit.SECONDS.sleep(2));
      result.setResult("result");
    });

    result.onTimeout(() -> log.warn("timeout {}", Thread.currentThread().getName()));

    result.onCompletion(() -> log.info("onCompletion {}", Thread.currentThread().getName()));

    result.onError(throwable -> log.error("onError {}", Thread.currentThread().getName(), throwable));
    return result;
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
  public void throwable(Throwable throwable, PrintWriter writer) {
    throwable.printStackTrace(writer);
    writer.flush();
  }

}
