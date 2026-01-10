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

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import infra.http.MediaType;
import infra.test.web.mock.MockMvc;
import infra.test.web.mock.MvcResult;
import infra.test.web.mock.setup.MockMvcBuilders;
import infra.web.annotation.GetMapping;
import infra.web.annotation.RestController;
import reactor.core.publisher.Flux;

import static infra.test.web.mock.request.MockMvcRequestBuilders.asyncDispatch;
import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.request;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;

/**
 * Tests with reactive return value types.
 *
 * @author Rossen Stoyanchev
 */
public class ReactiveReturnTypeTests {

  @Test
  public void sseWithFlux() throws Exception {

    MockMvc mockMvc = MockMvcBuilders.standaloneSetup(ReactiveController.class).build();

    MvcResult mvcResult = mockMvc.perform(get("/spr16869"))
            .andExpect(request().asyncStarted())
            .andExpect(status().isOk())
            .andReturn();

    mockMvc.perform(asyncDispatch(mvcResult))
            .andExpect(content().string("data:event0\n\ndata:event1\n\ndata:event2\n\n"));
  }

  @RestController
  static class ReactiveController {

    @GetMapping(path = "/spr16869", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    Flux<String> sseFlux() {
      return Flux.interval(Duration.ofSeconds(1)).take(3)
              .map(aLong -> String.format("event%d", aLong));
    }
  }

}
