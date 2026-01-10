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

import infra.stereotype.Controller;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.mock.samples.standalone.resultmatchers.UrlAssertionTests;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.annotation.RequestMapping;

import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.result.MockMvcResultMatchers.view;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * {@link MockMvcWebTestClient} equivalent of the MockMvc
 * {@link UrlAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class ViewNameAssertionTests {

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SimpleController())
                  .alwaysExpect(status().isOk())
                  .build();

  @Test
  public void testEqualTo() throws Exception {
    MockMvcWebTestClient.resultActionsFor(performRequest())
            .andExpect(view().name("mySpecialView"))
            .andExpect(view().name(equalTo("mySpecialView")));
  }

  @Test
  public void testHamcrestMatcher() throws Exception {
    MockMvcWebTestClient.resultActionsFor(performRequest())
            .andExpect(view().name(containsString("Special")));
  }

  private EntityExchangeResult<Void> performRequest() {
    return client.get().uri("/").exchange().expectBody().isEmpty();
  }

  @Controller
  private static class SimpleController {

    @RequestMapping("/")
    public String handle() {
      return "mySpecialView";
    }
  }
}
