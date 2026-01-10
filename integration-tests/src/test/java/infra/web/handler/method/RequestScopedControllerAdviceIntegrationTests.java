/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
