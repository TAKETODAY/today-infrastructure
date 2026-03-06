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

package infra.test.web.mock.samples.standalone;

import org.junit.jupiter.api.Test;

import java.util.List;

import infra.http.MediaType;
import infra.stereotype.Controller;
import infra.test.web.Person;
import infra.test.web.mock.MockMvc;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.SemanticApiVersionParser;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RequestParam;
import infra.web.annotation.ResponseBody;
import infra.web.client.ApiVersionInserter;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.jsonPath;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.standaloneSetup;

/**
 * Tests demonstrating the use of API version.
 *
 * @author Rossen Stoyanchev
 */
public class ApiVersionTests {

  @Test
  public void queryParameter() throws Exception {

    String header = "API-Version";

    DefaultApiVersionStrategy versionStrategy = new DefaultApiVersionStrategy(
            List.of(request -> request.getHeader(header)), new SemanticApiVersionParser(),
            true, null, true, null, null);

    MockMvc mockMvc = standaloneSetup(new PersonController())
            .setApiVersionStrategy(versionStrategy)
            .apiVersionInserter(ApiVersionInserter.forHeader(header))
            .build();

    mockMvc.perform(get("/search?name=George").accept(MediaType.APPLICATION_JSON).apiVersion(1.1))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$.name").value("George"));
  }

  @Controller
  private static class PersonController {

    @RequestMapping(path = "/search", version = "1.1")
    @ResponseBody
    public Person get(@RequestParam String name) {
      return new Person(name);
    }
  }

}
