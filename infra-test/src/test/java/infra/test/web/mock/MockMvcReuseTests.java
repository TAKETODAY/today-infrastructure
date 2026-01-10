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

package infra.test.web.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.session.config.EnableSession;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration tests that verify that {@link MockMvc} can be reused multiple
 * times within the same test method without side effects between independent
 * requests.
 * @author Sam Brannen
 * @author Rob Winch
 * @since 4.0
 */
@JUnitWebConfig
@TestInstance(Lifecycle.PER_CLASS)
class MockMvcReuseTests {

  private static final String HELLO = "hello";
  private static final String ENIGMA = "enigma";
  private static final String FOO = "foo";
  private static final String BAR = "bar";

  private final MockMvc mvc;

  MockMvcReuseTests(WebApplicationContext wac) {
    this.mvc = webAppContextSetup(wac).build();
  }

  @Test
  void sessionAttributesAreClearedBetweenInvocations() throws Exception {

    this.mvc.perform(get("/"))
            .andExpect(content().string(HELLO))
            .andExpect(request().sessionAttribute(FOO, nullValue()));

    this.mvc.perform(get("/").sessionAttr(FOO, BAR))
            .andExpect(content().string(HELLO))
            .andExpect(request().sessionAttribute(FOO, BAR));

    this.mvc.perform(get("/"))
            .andExpect(content().string(HELLO))
            .andExpect(request().sessionAttribute(FOO, nullValue()));
  }

  @Test
  void requestParametersAreClearedBetweenInvocations() throws Exception {
    this.mvc.perform(get("/"))
            .andExpect(content().string(HELLO));

    this.mvc.perform(get("/").param(ENIGMA, ""))
            .andExpect(content().string(ENIGMA));

    this.mvc.perform(get("/"))
            .andExpect(content().string(HELLO));
  }

  @Configuration
  @EnableWebMvc
  @EnableSession
  static class Config {

    @Bean
    MyController myController() {
      return new MyController();
    }
  }

  @RestController
  static class MyController {

    @GetMapping("/")
    String hello() {
      return HELLO;
    }

    @GetMapping(path = "/", params = ENIGMA)
    String enigma() {
      return ENIGMA;
    }
  }

}
