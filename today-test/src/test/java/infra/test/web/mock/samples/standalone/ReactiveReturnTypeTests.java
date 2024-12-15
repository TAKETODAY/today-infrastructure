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
