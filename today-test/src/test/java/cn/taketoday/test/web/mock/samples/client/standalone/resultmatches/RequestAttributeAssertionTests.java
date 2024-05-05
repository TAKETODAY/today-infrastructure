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

import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.mock.ResultActions;
import cn.taketoday.test.web.mock.client.MockMvcWebTestClient;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.annotation.GetMapping;

import static cn.taketoday.test.web.mock.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.mock.result.MockMvcResultMatchers.request;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockMvcWebTestClient equivalent of the MockMvc
 * {@link cn.taketoday.test.web.mock.samples.standalone.resultmatchers.RequestAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeAssertionTests {

  private final WebTestClient mainServletClient =
          MockMvcWebTestClient.bindToController(new SimpleController())
                  .defaultRequest(get("/"))
                  .build();

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SimpleController()).build();

  @Test
  void requestAttributeMatcher() throws Exception {
    performRequest(mainServletClient, "/1")
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
