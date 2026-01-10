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
