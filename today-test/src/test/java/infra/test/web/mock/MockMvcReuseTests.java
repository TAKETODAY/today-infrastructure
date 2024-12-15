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

package infra.test.web.mock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.session.config.EnableWebSession;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import infra.web.config.EnableWebMvc;
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
  @EnableWebSession
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
