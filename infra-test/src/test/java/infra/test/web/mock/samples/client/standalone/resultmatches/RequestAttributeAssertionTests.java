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

import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.mock.ResultActions;
import infra.test.web.mock.client.MockMvcWebTestClient;
import infra.test.web.reactive.server.EntityExchangeResult;
import infra.test.web.reactive.server.WebTestClient;
import infra.web.HandlerMatchingMetadata;
import infra.web.annotation.GetMapping;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockMvcWebTestClient equivalent of the MockMvc
 * {@link infra.test.web.mock.samples.standalone.resultmatchers.RequestAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeAssertionTests {

  private final WebTestClient mainMockClient =
          MockMvcWebTestClient.bindToController(new SimpleController())
                  .defaultRequest(get("/"))
                  .build();

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SimpleController()).build();

  @Test
  void requestAttributeMatcher() throws Exception {
    performRequest(mainMockClient, "/1")
            .andExpect(request().request(context -> {
              HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();

              assertThat(matchingMetadata).isNotNull();
              assertThat(matchingMetadata.getProducibleMediaTypes()).contains(MediaType.APPLICATION_JSON);
              assertThat(matchingMetadata.getProducibleMediaTypes()).doesNotContain(MediaType.APPLICATION_XML);

            }));

  }

  private ResultActions performRequest(WebTestClient client, String uri) {
    EntityExchangeResult<Void> result = client.get().uri(uri)
            .exchange()
            .expectStatus().isOk()
            .expectBody().isEmpty();

    return MockMvcWebTestClient.resultActionsFor(result);
  }

  @Controller
  private static class SimpleController {

    @GetMapping(path = "/{id}", produces = "application/json")
    String show() {
      return "view";
    }
  }

}
