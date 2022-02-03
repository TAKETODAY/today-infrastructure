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

package cn.taketoday.web.registry.annotation;

import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.event.EnableMethodEventDriven;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.util.MediaType;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.RequestBody;
import cn.taketoday.web.annotation.RequestMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.annotation.RestControllerAdvice;
import cn.taketoday.web.framework.WebApplication;
import cn.taketoday.web.framework.config.EnableTomcatHandling;
import cn.taketoday.http.HttpHeaders;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author TODAY 2021/4/22 22:32
 */
@Slf4j
@RestController
@RestControllerAdvice
@EnableRequestPathMapping
@RequestMapping("users")
@Import(RequestPathMappingHandlerMethodRegistryApp.AppConfig.class)
public class RequestPathMappingHandlerMethodRegistryApp {

  public static void main(String[] args) {
    WebApplication.run(RequestPathMappingHandlerMethodRegistryApp.class, args);
  }

  @GET("/index")
  public String index() {
    return "Hello";
  }

  @GET
  public String hello() {
    return "Hello";
  }

  @GET(params = "name=TODAY")
  public String today() {
    return "TODAY";
  }

  @GET(params = "name")
  public String params() {
    return "params";
  }

  @GET(params = "name", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Body paramsConsumes(@RequestBody Body body) {
    return body;
  }

  @GET("/header")
  public HttpHeaders header(HttpHeaders headers) {
    return headers;
  }

  @GET("/body/{name}/{age}")
  public Body index(String name, int age) {
    return new Body(name, age);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  static class Body {
    String name;
    int age;
  }

  @Configuration
  @EnableTomcatHandling
  @EnableMethodEventDriven
  static class AppConfig {

  }

}


