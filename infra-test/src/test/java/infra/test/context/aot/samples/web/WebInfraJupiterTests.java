/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.test.context.aot.samples.web;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.test.context.TestPropertySource;
import infra.test.context.junit.jupiter.web.JUnitWebConfig;
import infra.test.web.mock.MockMvc;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@JUnitWebConfig(classes = WebTestConfiguration.class, resourcePath = "classpath:META-INF/web-resources")
@TestPropertySource(properties = "test.engine = jupiter")
public class WebInfraJupiterTests {

  MockMvc mockMvc;

  @Autowired
  WebApplicationContext wac;

  @org.junit.jupiter.api.BeforeEach
  void setUpMockMvc() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @org.junit.jupiter.api.Test
  void controller(@Value("${test.engine}") String testEngine) throws Exception {
    assertThat(testEngine)
            .as("@Value").isEqualTo("jupiter");
    assertThat(wac.getEnvironment().getProperty("test.engine"))
            .as("Environment").isEqualTo("jupiter");

    mockMvc.perform(get("/hello"))
            .andExpectAll(status().isOk(), content().string("Hello, AOT!"));
  }

  @org.junit.jupiter.api.Test
  void resources() throws Exception {
    this.mockMvc.perform(get("/resources/Spring.js"))
            .andExpectAll(
                    content().contentType("text/javascript"),
                    content().string(containsString("Spring={};"))
            );
  }

}
