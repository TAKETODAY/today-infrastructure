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

package cn.taketoday.web.handler;

import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.interceptor.CorsHandlerInterceptor;
import cn.taketoday.web.interceptor.HandlerInterceptor;
import cn.taketoday.web.servlet.StandardWebServletApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY 2021/5/1 15:37
 * @since 3.0
 */
public class HandlerMethodBuilderTests {

  static class MyController {

    @GET("/get")
    public void get() {

    }
  }

  @EnableWebMvc
  static class AppConfig {

  }

  @Test
  public void testBuild() throws NoSuchMethodException {

    try (StandardApplicationContext context = new StandardWebServletApplicationContext()) {
      context.scan("cn.taketoday.web.handler");
      context.importBeans(AppConfig.class);

      final HandlerMethodBuilder<HandlerMethod> handlerMethodBuilder = new HandlerMethodBuilder<>(context);
      HandlerMethod handlerMethod = handlerMethodBuilder.build(new MyController(), MyController.class.getMethod("get"));
      assertThat(handlerMethod).isNotNull();
      assertThat(handlerMethod.getBean()).isNotNull();
      assertThat(handlerMethod.getMethod()).isNotNull();
      assertThat(handlerMethod.getInterceptors()).isNull();

      List<HandlerInterceptor> interceptors = new LinkedList<>();

      final CorsHandlerInterceptor interceptor = new CorsHandlerInterceptor();
      interceptors.add(interceptor);
      handlerMethod = handlerMethodBuilder.build(new MyController(), MyController.class.getMethod("get"), interceptors);
      assertThat(handlerMethod.getInterceptors()).isNotNull().hasSize(1);
      assertThat(handlerMethod.getInterceptors()[0]).isEqualTo(interceptor);
    }
  }

}
