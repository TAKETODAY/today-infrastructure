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

package cn.taketoday.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.mock.client.MockMvcWebTestClient;
import cn.taketoday.web.annotation.RequestMapping;

import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.forwardedUrlPattern;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.redirectedUrlPattern;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link cn.taketoday.test.web.mock.samples.standalone.resultmatchers.UrlAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class UrlAssertionTests {

  private final WebTestClient testClient =
          MockMvcWebTestClient.bindToController(new SimpleController()).build();

  @Test
  public void testRedirect() {
    testClient.get().uri("/persons")
            .exchange()
            .expectStatus().isFound()
            .expectHeader().location("/persons/1");
  }

  @Test
  public void testRedirectPattern() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.get().uri("/persons").exchange().expectBody().isEmpty();

    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(redirectedUrlPattern("/persons/*"));
  }

  @Test
  public void testForward() {
    testClient.get().uri("/")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("Forwarded-Url", "/home");
  }

  @Test
  public void testForwardPattern() throws Exception {
    EntityExchangeResult<Void> result =
            testClient.get().uri("/").exchange().expectBody().isEmpty();

    MockMvcWebTestClient.resultActionsFor(result)
            .andExpect(forwardedUrlPattern("/ho?e"));
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/persons")
    public String save() {
      return "redirect:/persons/1";
    }

    @RequestMapping("/")
    public String forward() {
      return "/home";
    }
  }
}
