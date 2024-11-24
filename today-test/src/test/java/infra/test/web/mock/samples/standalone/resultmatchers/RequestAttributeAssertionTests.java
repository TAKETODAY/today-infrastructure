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

package infra.test.web.mock.samples.standalone.resultmatchers;

import org.junit.jupiter.api.Test;

import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.mock.MockMvc;
import infra.web.HandlerMatchingMetadata;
import infra.web.annotation.RequestMapping;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Examples of expectations on created request attributes.
 *
 * @author Rossen Stoyanchev
 */
public class RequestAttributeAssertionTests {

  private final MockMvc mockMvc = standaloneSetup(new SimpleController()).build();

  @Test
  void requestAttributeMatcher() throws Exception {
    this.mockMvc.perform(get("/1"))
            .andExpect(request().request(context -> {
              HandlerMatchingMetadata matchingMetadata = context.getMatchingMetadata();

              assertThat(matchingMetadata).isNotNull();
              assertThat(matchingMetadata.getProducibleMediaTypes()).contains(MediaType.APPLICATION_JSON);
              assertThat(matchingMetadata.getProducibleMediaTypes()).doesNotContain(MediaType.APPLICATION_XML);

            }));

  }

  @Controller
  private static class SimpleController {

    @RequestMapping(path = "/{id}", produces = "application/json")
    String show() {
      return "view";
    }
  }

}
