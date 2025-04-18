/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.samples;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationEventPublisher;
import infra.context.event.EventListener;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.web.RequestContext;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;
import lombok.ToString;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 20:51
 */
@RestController
public class DemoController {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @EventListener
  public void event(MyEvent event) {
    log.info("event :{}", event);
  }

  @GET
  public String home() {
    return "hello world";
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

  @ToString
  static class MyEvent {
    final String name;

    MyEvent(String name) {
      this.name = name;
    }
  }

}
