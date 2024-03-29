/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.test.web.servlet.samples.client.standalone.resultmatches;

import org.junit.jupiter.api.Test;

import cn.taketoday.http.MediaType;
import cn.taketoday.stereotype.Controller;
import cn.taketoday.test.web.reactive.server.EntityExchangeResult;
import cn.taketoday.test.web.reactive.server.WebTestClient;
import cn.taketoday.test.web.servlet.ResultActions;
import cn.taketoday.test.web.servlet.client.MockMvcWebTestClient;
import cn.taketoday.web.HandlerMatchingMetadata;
import cn.taketoday.web.annotation.GetMapping;

import static cn.taketoday.test.web.servlet.request.MockMvcRequestBuilders.get;
import static cn.taketoday.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * MockMvcWebTestClient equivalent of the MockMvc
 * {@link cn.taketoday.test.web.servlet.samples.standalone.resultmatchers.RequestAttributeAssertionTests}.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeAssertionTests {

  private final WebTestClient mainServletClient =
          MockMvcWebTestClient.bindToController(new SimpleController())
                  .defaultRequest(get("/").servletPath("/main"))
                  .build();

  private final WebTestClient client =
          MockMvcWebTestClient.bindToController(new SimpleController()).build();

  @Test
  void requestAttributeMatcher() throws Exception {
    performRequest(mainServletClient, "/main/1")
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
