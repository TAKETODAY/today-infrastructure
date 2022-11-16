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

package cn.taketoday.web.handler;

import org.junit.jupiter.api.Test;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.stereotype.Component;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/11 17:53
 */
class DispatcherHandlerTests {

  @Configuration
  static class Config {

    @Component
    ParameterResolvingRegistry parameterResolvingRegistry() {
      return new ParameterResolvingRegistry();
    }

    @Component
    ReturnValueHandlerManager returnValueHandlerManager() {
      return new ReturnValueHandlerManager();
    }

  }

  @Test
  void init() {

    var context = new AnnotationConfigApplicationContext(Config.class);

    DispatcherHandler handler = new DispatcherHandler(context);
    assertThat(handler.getHandlerMapping()).isNull();
    assertThat(handler.getReturnValueHandler()).isNull();
    assertThat(handler.getExceptionHandler()).isNull();
    handler.init();

    assertThat(handler.getHandlerMapping()).isNotNull();
    assertThat(handler.getExceptionHandler()).isNotNull();
    assertThat(handler.getReturnValueHandler()).isNotNull();
  }

}