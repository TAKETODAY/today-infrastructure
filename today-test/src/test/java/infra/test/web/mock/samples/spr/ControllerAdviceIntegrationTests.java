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

package infra.test.web.mock.samples.spr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Scope;
import infra.stereotype.Component;
import infra.stereotype.Controller;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.setup.InternalResourceViewResolver;
import infra.ui.Model;
import infra.web.annotation.ControllerAdvice;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RequestParam;
import infra.web.bind.annotation.ModelAttribute;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.context.annotation.RequestScope;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.forwardedUrl;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ControllerAdvice @ControllerAdvice}.
 *
 * <p>Introduced in conjunction with
 * <a href="https://github.com/spring-projects/spring-framework/issues/24017">gh-24017</a>.
 *
 * @author Sam Brannen
 */
@JUnitWebConfig
class ControllerAdviceIntegrationTests {

  MockMvc mockMvc;

  @BeforeEach
  void setUpMockMvc(WebApplicationContext wac) {
    this.mockMvc = webAppContextSetup(wac)
            .build();
    resetCounters();
  }

  @Test
  void controllerAdviceIsAppliedOnlyOnce() throws Exception {
    this.mockMvc.perform(get("/test").param("requestParam", "foo"))//
            .andExpect(status().isOk())//
            .andExpect(forwardedUrl("singleton:1;prototype:1;request-scoped:1;requestParam:foo"));

    assertThat(SingletonControllerAdvice.invocationCount).hasValue(1);
    assertThat(PrototypeControllerAdvice.invocationCount).hasValue(1);
    assertThat(RequestScopedControllerAdvice.invocationCount).hasValue(1);
  }

  @Test
  void prototypeAndRequestScopedControllerAdviceBeansAreNotCached() throws Exception {
    this.mockMvc.perform(get("/test").param("requestParam", "foo"))//
            .andExpect(status().isOk())//
            .andExpect(forwardedUrl("singleton:1;prototype:1;request-scoped:1;requestParam:foo"));

    // singleton @ControllerAdvice beans should not be instantiated again.
    assertThat(SingletonControllerAdvice.instanceCount).hasValue(0);
    // prototype and request-scoped @ControllerAdvice beans should be instantiated once per request.
    assertThat(PrototypeControllerAdvice.instanceCount).hasValue(1);
    assertThat(RequestScopedControllerAdvice.instanceCount).hasValue(1);

    this.mockMvc.perform(get("/test").param("requestParam", "bar"))//
            .andExpect(status().isOk())//
            .andExpect(forwardedUrl("singleton:2;prototype:2;request-scoped:2;requestParam:bar"));

    // singleton @ControllerAdvice beans should not be instantiated again.
    assertThat(SingletonControllerAdvice.instanceCount).hasValue(0);
    // prototype and request-scoped @ControllerAdvice beans should be instantiated once per request.
    assertThat(PrototypeControllerAdvice.instanceCount).hasValue(2);
    assertThat(RequestScopedControllerAdvice.instanceCount).hasValue(2);
  }

  private void resetCounters() {
    SingletonControllerAdvice.invocationCount.set(0);
    SingletonControllerAdvice.instanceCount.set(0);
    PrototypeControllerAdvice.invocationCount.set(0);
    PrototypeControllerAdvice.instanceCount.set(0);
    RequestScopedControllerAdvice.invocationCount.set(0);
    RequestScopedControllerAdvice.instanceCount.set(0);
  }

  @Configuration
  @EnableWebMvc
  static class Config {

    @Component
    InternalResourceViewResolver viewResolver() {
      return new InternalResourceViewResolver();
    }

    @Bean
    TestController testController() {
      return new TestController();
    }

    @Bean
    SingletonControllerAdvice singletonControllerAdvice() {
      return new SingletonControllerAdvice();
    }

    @Bean
    @Scope("prototype")
    PrototypeControllerAdvice prototypeControllerAdvice() {
      return new PrototypeControllerAdvice();
    }

    @Bean
    @RequestScope
    RequestScopedControllerAdvice requestScopedControllerAdvice() {
      return new RequestScopedControllerAdvice();
    }
  }

  @ControllerAdvice
  static class SingletonControllerAdvice {

    static final AtomicInteger instanceCount = new AtomicInteger();
    static final AtomicInteger invocationCount = new AtomicInteger();

    {
      instanceCount.incrementAndGet();
    }

    @ModelAttribute
    void initModel(Model model) {
      model.addAttribute("singleton", invocationCount.incrementAndGet());
    }
  }

  @ControllerAdvice
  static class PrototypeControllerAdvice {

    static final AtomicInteger instanceCount = new AtomicInteger();
    static final AtomicInteger invocationCount = new AtomicInteger();

    {
      instanceCount.incrementAndGet();
    }

    @ModelAttribute
    void initModel(Model model) {
      model.addAttribute("prototype", invocationCount.incrementAndGet());
    }
  }

  @ControllerAdvice
  static class RequestScopedControllerAdvice {

    static final AtomicInteger instanceCount = new AtomicInteger();
    static final AtomicInteger invocationCount = new AtomicInteger();

    {
      instanceCount.incrementAndGet();
    }

    @ModelAttribute
    void initModel(@RequestParam String requestParam, Model model) {
      model.addAttribute("requestParam", requestParam);
      model.addAttribute("request-scoped", invocationCount.incrementAndGet());
    }
  }

  @Controller
  static class TestController {

    @GetMapping("/test")
    String get(Model model) {
      return "singleton:" + model.getAttribute("singleton") +
              ";prototype:" + model.getAttribute("prototype") +
              ";request-scoped:" + model.getAttribute("request-scoped") +
              ";requestParam:" + model.getAttribute("requestParam");
    }
  }

}
