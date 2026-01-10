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

import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Value;
import infra.test.context.ContextConfiguration;
import infra.test.context.TestPropertySource;
import infra.test.context.junit4.InfraRunner;
import infra.test.context.web.WebAppConfiguration;
import infra.test.web.mock.MockMvc;
import infra.web.mock.WebApplicationContext;

import static infra.test.web.mock.request.MockMvcRequestBuilders.get;
import static infra.test.web.mock.result.MockMvcResultMatchers.content;
import static infra.test.web.mock.result.MockMvcResultMatchers.status;
import static infra.test.web.mock.setup.MockMvcBuilders.webAppContextSetup;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(InfraRunner.class)
@ContextConfiguration(classes = WebTestConfiguration.class)
@WebAppConfiguration
@TestPropertySource(properties = "test.engine = vintage")
public class WebInfraVintageTests {

  MockMvc mockMvc;

  @Autowired
  WebApplicationContext wac;

  @Value("${test.engine}")
  String testEngine;

  @org.junit.Before
  public void setUpMockMvc() {
    this.mockMvc = webAppContextSetup(this.wac).build();
  }

  @org.junit.Test
  public void test() throws Exception {
    assertThat(testEngine)
            .as("@Value").isEqualTo("vintage");
    assertThat(wac.getEnvironment().getProperty("test.engine"))
            .as("Environment").isEqualTo("vintage");

    mockMvc.perform(get("/hello"))
            .andExpectAll(status().isOk(), content().string("Hello, AOT!"));
  }

}
