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

package infra.test.web.mock.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import java.net.URL;

import infra.stereotype.Controller;
import infra.test.web.mock.ResultActions;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.RedirectModel;
import infra.web.annotation.PostMapping;

import static infra.test.web.mock.result.MockMvcResultMatchers.flash;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.FlashAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class FlashAttributeAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new PersonController())
                  .alwaysExpect(status().isFound())
                  .alwaysExpect(flash().attributeCount(3))
                  .build();

  @Test
  void attributeCountWithWrongCount() {
    assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> performRequest().andExpect(flash().attributeCount(1)))
            .withMessage("RedirectModel size expected:<1> but was:<3>");
  }

  @Test
  void attributeExists() throws Exception {
    performRequest().andExpect(flash().attributeExists("one", "two", "three"));
  }

  @Test
  void attributeEqualTo() throws Exception {
    performRequest()
            .andExpect(flash().attribute("one", "1"))
            .andExpect(flash().attribute("two", 2.222))
            .andExpect(flash().attribute("three", new URL("https://example.com")));
  }

  @Test
  void attributeMatchers() throws Exception {
    performRequest()
            .andExpect(flash().attribute("one", containsString("1")))
            .andExpect(flash().attribute("two", closeTo(2, 0.5)))
            .andExpect(flash().attribute("three", notNullValue()))
            .andExpect(flash().attribute("one", equalTo("1")))
            .andExpect(flash().attribute("two", equalTo(2.222)))
            .andExpect(flash().attribute("three", equalTo(new URL("https://example.com"))));
  }

  private ResultActions performRequest() {
    EntityExchangeResult<Void> result = client.post().uri("/persons").exchange().expectBody().isEmpty();
    return MockMvcWebTestClient.resultActionsFor(result);
  }

  @Controller
  private static class PersonController {

    @PostMapping("/persons")
    String save(RedirectModel redirectAttrs) throws Exception {
      redirectAttrs.addAttribute("one", "1");
      redirectAttrs.addAttribute("two", 2.222);
      redirectAttrs.addAttribute("three", new URL("https://example.com"));
      return "redirect:/person/1";
    }
  }

}
