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

package infra.web.handler.method;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.core.Ordered;
import infra.core.annotation.Order;
import infra.mock.web.MockContextImpl;
import infra.web.annotation.ControllerAdvice;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.context.annotation.RequestScope;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for request-scoped {@link ControllerAdvice @ControllerAdvice} beans.
 *
 * @author Sam Brannen
 */
class RequestScopedControllerAdviceIntegrationTests {

  @Test
    // gh-23985
  void loadContextWithRequestScopedControllerAdvice() {
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.setMockContext(new MockContextImpl());
    context.register(Config.class);

    assertThatCode(context::refresh).doesNotThrowAnyException();

    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(context);
    assertThat(adviceBeans).hasSize(1);
    assertThat(adviceBeans.get(0))//
            .returns(RequestScopedControllerAdvice.class, ControllerAdviceBean::getBeanType)//
            .returns(42, ControllerAdviceBean::getOrder);

    context.close();
  }

  @Configuration
  @EnableWebMvc
  static class Config {

    @Bean
    @RequestScope
    RequestScopedControllerAdvice requestScopedControllerAdvice() {
      return new RequestScopedControllerAdvice();
    }
  }

  @ControllerAdvice
  @Order(42)
  static class RequestScopedControllerAdvice implements Ordered {

    @Override
    public int getOrder() {
      return 99;
    }
  }

}
