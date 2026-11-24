/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.web.mock.client;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import infra.format.support.DefaultFormattingConversionService;
import infra.http.converter.StringHttpMessageConverter;
import infra.test.web.reactive.server.WebTestClient;
import infra.validation.Validator;
import infra.web.HandlerInterceptor;
import infra.web.annotation.GET;
import infra.web.annotation.RestController;
import infra.web.accept.ContentNegotiationManager;
import infra.web.handler.method.RequestMappingHandlerMapping;
import infra.web.i18n.AcceptHeaderLocaleResolver;
import infra.web.view.View;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StandaloneMockMvcSpecTests {

  @Test
  void customizedStandaloneSpecBuildsAWorkingClient() {
    AtomicInteger mappings = new AtomicInteger();
    StandaloneMockMvcSpec spec = new StandaloneMockMvcSpec(EchoController.class);
    assertThat(spec.controllerAdvice().messageConverters(new StringHttpMessageConverter())
            .validator(mock(Validator.class)).conversionService(new DefaultFormattingConversionService())
            .interceptors(new HandlerInterceptor() { }).mappedInterceptors(new String[] { "/hello" }, new HandlerInterceptor() { })
            .contentNegotiationManager(new ContentNegotiationManager()).asyncRequestTimeout(Duration.ofSeconds(2))
            .customArgumentResolvers().customReturnValueHandlers().handlerExceptionHandlers()
            .viewResolvers().singleView(mock(View.class)).localeResolver(new AcceptHeaderLocaleResolver())
            .useTrailingSlashPatternMatch(false).placeholderValue("path", "hello").customHandlerMapping(() -> {
              mappings.incrementAndGet();
              return new RequestMappingHandlerMapping();
            })).isSameAs(spec);
    WebTestClient client = spec.configureClient().baseUrl("https://example.org").build();
    client.get().uri("/hello").exchange().expectStatus().isOk().expectBody(String.class).isEqualTo("hello");
    assertThat(mappings).hasValue(1);
  }

  @RestController
  static class EchoController {
    @GET("/${path}")
    String hello() {
      return "hello";
    }
  }

}
